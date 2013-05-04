(ns urdar.client.main
  "Entry point of client-side code."
  (:require [urdar.crossovers.validation :as v]
            [cljs.reader :as r]
            [clojure.string :as s]
            [enfocus.core :as ef]
            [enfocus.events :as events]
            [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable]
            [shoreleave.remotes.request :as remote]
            [dommy.template :as template]
            [shoreleave.brepl :as brepl]
            [goog.async.Throttle])
  (:require-macros [enfocus.macros :as em]))

;;; TODO extract namespaces

;;; ## State management

;;; TODO bookmarks-to-fetch should be based on screen height
(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 20 :tag nil}))

(defn bookmark-fetched! [_]
  (swap! state update-in [:bookmarks-fetched] inc))

(defn bookmark-removed! [_]
  (swap! state update-in [:bookmarks-fetched] dec))

(defn set-tag! [{:keys [tag]}]
  (swap! state assoc :tag tag)
  (swap! state assoc :bookmarks-fetched 0))

;;; ## Utils

(defn document-height
  "Retrieves height of document."
  []
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max (.-scrollHeight body) (.-offsetHeight body) (.-clientHeight html)
         (.-scrollHeight html) (.-offsetHeight html))))

(defn get-parent
  "Retrieves parent of a current node."
  [node]
  (.-parentNode node))

(def get-grandparent (comp get-parent get-parent))

(defn generate-id [link] (:bookmarks-fetched @state))

;;; Adds a validation failed notification to a new link adder.
(em/defaction new-link-validation-failed [error-msg]
  ["#add-bookmark"] (ef/add-class "error")
  ["#add-bookmark-error"] (ef/do-> (ef/content error-msg)
                                   (ef/remove-class "hidden")))

;;; Removes a validation failed notification to a new link adder.
(em/defaction new-link-validation-succeeded [_]
  ["#add-bookmark-error"] (ef/add-class "hidden")
  ["#add-bookmark"] (ef/remove-class "error"))

(em/defaction remove-all-bookmarks []
  [".bookmark"] (ef/remove-node))

;;; ## PubSub-related utility variables/functions

(defrecord BookmarkAddedEvent [link new? tags])
(defrecord BookmarkRemovedEvent [node])
(defrecord TagMenuChange [tag reset-menu?])
(defrecord TagAddedEvent [node link tag update-tag-menu?])
(defrecord TagRemovedEvent [node])
(defrecord TagChangedEvent [tag])

(def ^{:private true} bus (pbus/bus))

(def bookmarks-topic (pubsub/topicify :bookmarks))
(def publish-bookmark (partial pubsub/publish bus bookmarks-topic))
(def subscribe-to-bookmarks (partial pubsub/subscribe bus bookmarks-topic))

(def bookmarks-removed-topic (pubsub/topicify :bookmarks-removed))
(def publish-bookmark-removed
  (partial pubsub/publish bus bookmarks-removed-topic))
(def subscribe-to-bookmarks-removed
  (partial pubsub/subscribe bus bookmarks-removed-topic))

(def tags-menu-topic (pubsub/topicify :tags-menu))
(def publish-tags-menu-change (partial pubsub/publish bus tags-menu-topic))
(def subscribe-to-tags-menu-changes
  (partial pubsub/subscribe bus tags-menu-topic))

(def tags-topic (pubsub/topicify :tags))
(def publish-tag (partial pubsub/publish bus tags-topic))
(def subscribe-to-tags (partial pubsub/subscribe bus tags-topic))

(def tags-removed-topic (pubsub/topicify :tags-removed))
(def publish-tag-removed
  (partial pubsub/publish bus tags-removed-topic))
(def subscribe-to-tags-removed
  (partial pubsub/subscribe bus tags-removed-topic))

(def tag-changed-topic (pubsub/topicify :change-tag))
(def publish-tag-changed (partial pubsub/publish bus tag-changed-topic))
(def subscribe-to-tag-changed
  (partial pubsub/subscribe bus tag-changed-topic))

;;; ## Interactions with server

;;; TODO more elaborate error handling for remotes

(defn fetch-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  ([]
     (let [{:keys [bookmarks-fetched bookmarks-to-fetch tag]} @state]
       (fetch-bookmarks tag bookmarks-fetched bookmarks-to-fetch)))
  ([tag bookmarks-fetched bookmarks-to-fetch]
     (remote/request
      [:get (str "/_/bookmarks/" bookmarks-fetched "/"
                 bookmarks-to-fetch "/" tag)]
      :headers {"Content-Type" "application/edn;charset=utf-8"}
      :on-success
      (fn [{body :body}]
        (let [bookmarks (r/read-string body)]
          (doseq [b bookmarks]
            (publish-bookmark (->BookmarkAddedEvent (:link b) false
                                               (:tags b))))))
      :on-error
      (fn [_] (ef/log-debug "Error while downloading bookmarks.")))))

(defn fetch-tags
  "Retrieves all currently existing tags of user from DB."
  [{:keys [update-tag-menu?]}]
  (when update-tag-menu?
    (remote/request
     [:get "/_/tags"]
     :headers {"Content-Type" "application/edn;charset=utf-8"}
     :on-success
     (fn [{tags-str :body}]
       (let [tags (r/read-string tags-str)]
         (publish-tags-menu-change (->TagMenuChange nil true))
         (doseq [tag tags]
           (publish-tags-menu-change (->TagMenuChange tag false)))))
     :on-error
     (fn [_] (ef/log-debug "Error while downloading tags menu.")))))

(defn add-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:post "/_/bookmarks"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link})
   :on-success
   (fn [{bookmark :body}]
     (let [b (r/read-string bookmark)]
       (publish-bookmark (->BookmarkAddedEvent (:link b) true []))))
   :on-error
   (fn [{status :status}]
     (condp = status
       409 (new-link-validation-failed "Bookmark already exists.")
       422 (new-link-validation-failed "Incorrect URL.")))))

(defn remove-bookmark!
  "Adds bookmark for current user in DB."
  [link node]
  (remote/request
   [:delete "/_/bookmarks"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link})
   :on-success
   (fn [_] (publish-bookmark-removed (->BookmarkRemovedEvent node)))
   :on-error
   (fn [_] (ef/log-debug "Error while deleting bookmark."))))

(defn add-tag!
  "Tags a link for current user."
  [tag link node]
  (remote/request
   [:post "/_/tags"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :tag tag})
   :on-success
   (fn [_] (publish-tag (->TagAddedEvent node link tag true)))
   :on-error
   (fn [_] (ef/log-debug "error"))))

(defn remove-tag!
  "Untags a link for current user."
  [tag link node]
  (remote/request
   [:delete "/_/tags"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :tag tag})
   :on-success
   (fn [_] (publish-tag-removed (->TagRemovedEvent node)))
   :on-error
   (fn [_] (ef/log-debug "Error while deleting tag."))))

;;; ## DOM handling/rendering code

(defn read-link-to-add
  "Reads a current value of link in text field."
  []
  (ef/from js/document
           :link ["#control-panel #add-bookmark #link-to-add"]
           (ef/get-prop :value)))

(defn read-tag-to-add
  "Reads a current value of link in text field."
  [node]
  (ef/from node :tag [".tag"] (ef/get-prop :value)))

(defn read-tag-to-remove
  "Reads a current value of link in text field."
  [node]
  (ef/from node :tag [".tag"] (ef/get-text)))

(defn add-tag-popup
  [popup-id link]
  (template/node
   [:div.modal.hide {:data-keyboard true :id popup-id}
    [:div.modal-header
     [:button.close {:data-dismiss "modal"} "x"]]
    [:div.row-fluid
     [:div.modal-body
      [:div.span8.offset3 [:input.tag {:type "text" :placeholder "tag"}]]
      [:div.span8.offset4
       [:button.btn.btn-primary {:type "submit"} "Add tag"]]]]]))

(defn tag-link [tag]
  (let [[tag-link tag-text] (if tag
                              [(str "#" tag) tag]
                              ["#" "Remove tag filtering"])]
    (template/node
     [:span [:a.set-tag! {:href tag-link} [:span.tag tag-text]]])))

(defn tag-element
  "Creates a HTML element for tag."
  [tag]
  (template/node
   [:span
    [:span.label (tag-link tag) " "
     [:a.remove-tag! {:href "#delete-tag"} [:i.icon-remove]]]
    " "]))

;;; TODO show title of page, not link itself
(defn bookmark-div
  "Creates a bookmark HTML element."
  [link tags bookmark-id popup-id]
  (template/node
   [:div.bookmark.well.well-small {:id bookmark-id}
    [:div [:a {:href link :target "_blank"} link]
     [:button.close.btn-danger.delete-bookmark! "Delete"]]
    [:div.tags [:i.icon-tags] "Tags: "
     [:span.tags-list]
     [:a.add-tag! {:href "#add-tags" :data-toggle "modal"
                   :data-target (str "#" popup-id)}
      [:i.icon-plus]]
     (add-tag-popup popup-id link)]]))

(defn render-tag [{:keys [node link tag]}]
  (let [tag-node (tag-element tag)]
    (ef/at node [".tags-list"] (ef/prepend tag-node))
    (ef/at tag-node
           [".remove-tag!"]
           (events/listen :click (fn [event] (remove-tag! tag link tag-node)))

           [".set-tag!"]
           (events/listen
            :click
            (fn [event] (publish-tag-changed (->TagChangedEvent tag)))))))

(defn refetch-bookmarks [_]
  (remove-all-bookmarks)
  (fetch-bookmarks))

;;; TODO tag validation
;;; Render a bookmark.
(defn render-bookmark [{:keys [link new? tags]}]
  (let [id (generate-id link)
        popup-id (str "modal-" id)
        bookmark-id (str "bookmark-" id)
        n (bookmark-div link tags bookmark-id popup-id)]
    (ef/at js/document
           ["#bookmarks"]
           (let [adder (if new? ef/prepend ef/append)] (adder n))

           [(str "#" bookmark-id " .delete-bookmark!")]
           (events/listen :click (fn [event] (remove-bookmark! link n)))

           [(str "#" popup-id " .btn")]
           (events/listen
            :click
            (fn [event]
              (let [tag
                    (:tag
                     (read-tag-to-add (get-grandparent (.-currentTarget event))))]
                (add-tag! tag link n)))))
    (when (not (empty? tags))
      (doall (map #(publish-tag (->TagAddedEvent n link % false)) tags)))))

;;; TODO remove handler when deleting
(defn remove-node [{:keys [node]}] (ef/at node (ef/remove-node)))

(defn clean-tags-menu [{:keys [reset-menu?]}]
  (when reset-menu?
    (ef/at js/document
           ["#tags"]
           (ef/content ""))))

;;; TODO ability to delete tags completely
;;; TODO 'unselect' link after click
(defn render-tag-menu-element [{:keys [tag reset-menu?]}]
  (let [tag-node (tag-link tag)]
    (ef/at js/document
           ["#tags"]
           (ef/prepend tag-node " "))
    (ef/at tag-node
           [".set-tag!"]
           (events/listen
            :click
            (fn [event] (publish-tag-changed (->TagChangedEvent tag)))))))

;;; ## Events

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-new-link-click-handler []
  ["#add-bookmark!"]
  (events/listen
   :click
   (fn [event]
     (let [link (-> (:link (read-link-to-add))
                    s/trim)
           link (if (and (not= (.indexOf link "http://") 0)
                         (not= (.indexOf link "https://") 0))
                  (str "http://" link)
                  link)]
       (if (v/valid-url? link)
         (add-bookmark! link)
         (new-link-validation-failed "Incorrect URL."))))))

;;; ## Endless Scroll

(def throttle (goog.async.Throttle. fetch-bookmarks 1500))
(defn show-next-page []
  (.fire throttle))

;;; TODO stop scrolling when end reached
(defn on-scroll
  "Load next 'page' of bookmarks (if any left) when bottom of page is
   reached."
  []
  (let [max-height (document-height)
        scrolled (+ (.-pageYOffset js/window) (.-innerHeight js/window))
        left-to-scroll (- max-height scrolled)]
    (when (= left-to-scroll 0) (show-next-page))))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (subscribe-to-bookmarks bookmark-fetched!)
  (subscribe-to-bookmarks new-link-validation-succeeded)
  (subscribe-to-bookmarks render-bookmark)
  (subscribe-to-bookmarks-removed remove-node)
  (subscribe-to-tags-menu-changes clean-tags-menu)
  (subscribe-to-tags-menu-changes render-tag-menu-element)
  (subscribe-to-tags render-tag)
  (subscribe-to-tags fetch-tags)
  (subscribe-to-tags-removed remove-node)
  (subscribe-to-tag-changed set-tag!)
  (subscribe-to-tag-changed refetch-bookmarks)
  (add-new-link-click-handler)
  (fetch-tags {:update-tag-menu? true})
  (fetch-bookmarks)
  (set! (.-onscroll js/window) on-scroll)
  (brepl/connect))

(set! (.-onload js/window) start)

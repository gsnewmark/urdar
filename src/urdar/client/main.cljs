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
(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 20}))

(defn bookmark-fetched! [_]
  (swap! state update-in [:bookmarks-fetched] inc))

(defn bookmark-removed! [_]
  (swap! state update-in [:bookmarks-fetched] dec))

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

;;; Adds a validation failed notification to a new link adder.
(em/defaction new-link-validation-failed [error-msg]
  ["#add-bookmark"] (ef/add-class "error")
  ["#add-bookmark-error"] (ef/do-> (ef/content error-msg)
                                   (ef/remove-class "hidden")))

;;; Removes a validation failed notification to a new link adder.
(em/defaction new-link-validation-succeeded [_]
  ["#add-bookmark-error"] (ef/add-class "hidden")
  ["#add-bookmark"] (ef/remove-class "error"))

;;; ## PubSub-related utility variables/functions

(defrecord BookmarkEvent [link new? tags])

(def ^{:private true} bus (pbus/bus))
(def bookmarks-topic (pubsub/topicify :bookmarks))
(def publish-bookmark (partial pubsub/publish bus bookmarks-topic))
(def subscribe-to-bookmarks (partial pubsub/subscribe bus bookmarks-topic))

;;; ## Interactions with server

;;; TODO correctly receive non-english characters

(defn fetch-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  ([]
     (let [{:keys [bookmarks-fetched bookmarks-to-fetch]} @state]
       (fetch-bookmarks bookmarks-fetched bookmarks-to-fetch)))
  ([bookmarks-fetched bookmarks-to-fetch]
     (remote/request
      [:get (str "/_/bookmarks/" bookmarks-fetched "/" bookmarks-to-fetch)]
      :headers {"Content-Type" "application/edn;charset=utf-8"}
      :on-success
      (fn [{body :body}]
        (let [bookmarks (r/read-string body)]
          (doseq [b bookmarks]
            (publish-bookmark (->BookmarkEvent (:link b) false (:tags b)))))))))

(defn add-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:post "/_/add-bookmark"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link})
   :on-success
   (fn [{bookmark :body}]
     (let [b (r/read-string bookmark)]
       (publish-bookmark (->BookmarkEvent (:link b) true []))))
   :on-error
   (fn [{status :status}]
     (condp = status
       409 (new-link-validation-failed "Bookmark already exists.")
       422 (new-link-validation-failed "Incorrect URL.")))))

;;; TODO on-success, on-error
(defn remove-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:delete "/_/delete-bookmark"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link})))

;;; TODO on-success, on-error
(defn add-tag!
  "Adds bookmark for current user in DB."
  [tag link]
  (remote/request
   [:post "/_/add-tag"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :tag tag})))

;;; ## DOM handling/rendering code

(defn read-link-to-add
  "Reads a current value of link in text field."
  []
  (ef/from js/document
           :link ["#control-panel #add-bookmark #link-to-add"]
           (ef/get-prop :value)))

;;; TODO button to delete tag
(defn tag-div
  "Creates a HTML element for tag."
  [tag]
  [:li [:span.label tag]])

;;; TODO show title of page, not link itself
(defn bookmark-div
  "Creates a bookmark HTML element."
  [link tags]
  (template/node
   [:div.bookmark.well.well-small
    [:div [:a {:href link :target "_blank"} link]
     [:button.close.btn-danger.delete-bookmark! "Delete"]]
    [:div.tags
     [:ul.inline
      [:li [:button.add-tag! [:i.icon-plus]]]
      (when-not (empty? tags) (doall (map tag-div tags)))]]]))

;;; TODO remove handler when deleting
;;; Render a bookmark.
(defn render-bookmark [{:keys [link new? tags]}]
  (let [n (bookmark-div link tags)]
    (ef/at js/document
           ["#bookmarks"]
           (let [adder (if new? ef/prepend ef/append)]
             (adder n)))
    (ef/at
     n
     [".delete-bookmark!"]
     (events/listen
      :click
      (fn [event]
        (remove-bookmark! link)
        ;; TODO use pubsub
        (bookmark-removed! event)
        (ef/at (get-grandparent (.-currentTarget event)) (ef/remove-node))))
     [".add-tag!"]
     (events/listen
      :click
      ;; TODO use pubsub
      ;; TODO actually specify tag
      (fn [event] (add-tag! "tag" link))))))

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
  (add-new-link-click-handler)
  (brepl/connect)
  (fetch-bookmarks)
  (set! (.-onscroll js/window) on-scroll))

(set! (.-onload js/window) start)

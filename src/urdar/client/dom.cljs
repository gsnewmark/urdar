(ns urdar.client.dom
  "Various functions that deal with representation of page."
  (:require [urdar.client.pubsub :as p]
            [urdar.client.remote :as r]
            [urdar.client.state :as s]
            [urdar.crossovers.validation :as v]
            [clojure.string :as string]
            [dommy.template :as template]
            [enfocus.core :as ef]
            [enfocus.events :as events])
  (:require-macros [enfocus.macros :as em]))

;;; ## Utils

(defn get-parent
  "Retrieves parent of a current node."
  [node]
  (.-parentNode node))

(def get-grandparent (comp get-parent get-parent))

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

(em/defaction remove-all-bookmarks []
  [".bookmark"] (ef/remove-node))

;;; TODO remove handler when deleting
(defn remove-node [{:keys [node]}] (ef/at node (ef/remove-node)))

(defn clean-tags-menu [{:keys [reset-menu?]}]
  (when reset-menu?
    (ef/at js/document
           ["#tags"]
           (ef/content ""))))

;;; ## Templates

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

;;; ## Rendering (subscribers for pubsub)

(defn render-tag [{:keys [node link tag]}]
  (let [tag-node (tag-element tag)]
    (ef/at node [".tags-list"] (ef/prepend tag-node))
    (ef/at tag-node
           [".remove-tag!"]
           (events/listen :click (fn [event] (r/remove-tag! tag link tag-node)))

           [".set-tag!"]
           (events/listen
            :click
            (fn [event] (p/publish-tag-changed (p/->TagChangedEvent tag)))))))

;;; TODO tag validation
;;; Render a bookmark.
(defn render-bookmark [{:keys [link new? tags]}]
  (let [id (s/generate-id link)
        popup-id (str "modal-" id)
        bookmark-id (str "bookmark-" id)
        n (bookmark-div link tags bookmark-id popup-id)]
    (ef/at js/document
           ["#bookmarks"]
           (let [adder (if new? ef/prepend ef/append)] (adder n))

           [(str "#" bookmark-id " .delete-bookmark!")]
           (events/listen :click (fn [event] (r/remove-bookmark! link n)))

           [(str "#" popup-id " .btn")]
           (events/listen
            :click
            (fn [event]
              (let [tag
                    (:tag
                     (read-tag-to-add (get-grandparent (.-currentTarget event))))]
                (r/add-tag! tag link n)))))
    (when (not (empty? tags))
      (doall (map #(p/publish-tag (p/->TagAddedEvent n link % false)) tags)))))

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
            (fn [event] (p/publish-tag-changed (p/->TagChangedEvent tag)))))))

;;; ## Event handlers

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-new-link-click-handler []
  ["#add-bookmark!"]
  (events/listen
   :click
   (fn [event]
     (let [link (-> (:link (read-link-to-add))
                    string/trim)
           link (if (and (not= (.indexOf link "http://") 0)
                         (not= (.indexOf link "https://") 0))
                  (str "http://" link)
                  link)]
       (if (v/valid-url? link)
         (r/add-bookmark! link)
         (r/new-link-validation-failed "Incorrect URL."))))))

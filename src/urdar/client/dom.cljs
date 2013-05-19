(ns urdar.client.dom
  "Various functions that deal with representation of page."
  (:require [urdar.client.pubsub :as p]
            [urdar.client.remote :as r]
            [urdar.client.state :as s]
            [urdar.crossovers.validation :as v]
            [clojure.string :as string]
            [goog.dom.classes :as classes]
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

(defn generate-enter-up-listener
  [f]
  (events/listen
   :keyup (fn [e] (when (= 13 (or (.-keyCode e) (.-which e))) (f e)))))

(defn read-link-to-add
  "Reads a current value of link in text field."
  []
  (string/trim
   (:link (ef/from js/document
                   :link ["#control-panel #add-bookmark #link-to-add"]
                   (ef/get-prop :value)))))

(defn read-tag-to-add
  "Reads a current value of link in new tag text field."
  [node]
  (string/trim (:tag (ef/from node :tag [".new-tag"] (ef/get-prop :value)))))

(defn read-note
  "Reads a current value of note for a given bookmark."
  [node]
  (string/trim
   (:note (ef/from node :note ["textarea.note"] (ef/get-prop :value)))))

(defn clear-input-element
  "Clears text input field."
  [selector {:keys [node] :or {node js/document}}]
  (ef/at node [selector] (ef/set-prop :value "")))

(em/defaction remove-all-bookmarks []
  [".bookmark"] (ef/remove-node))

;;; TODO remove handler when deleting
(defn remove-node [{:keys [node]}] (ef/at node (ef/remove-node)))

(defn remove-tag-node [{:keys [node tag] :as e}]
  (if (s/tag-selected? tag)
    (p/publish-bookmark-removed
     (p/->BookmarkRemovedEvent (get-parent (get-grandparent node)) false))
    (remove-node e)))

(defn clean-tags-menu [{:keys [reset-menu?]}]
  (when reset-menu?
    (ef/at js/document
           ["#tags"]
           (ef/content ""))))

(em/defaction tag-filter-selected [{:keys [tag]}]
  [".tag-filter"] (ef/remove-class "btn-success")
  [(str "#" tag)] (when tag (ef/add-class "btn-success")))

;;; ## Templates

(def recommendations-are-loading
  (template/node
   [:span
    [:i.icon-refresh.icon-spin.icon-large]
    "Please wait while recommendations are prepared..."]))

(defn recommendation-node
  [link]
  (template/node [:li [:a {:href link :target "_blank"} link] " "
                  [:a {:href "#add-bookmark"} [:i.icon-bookmark]]]))

(defn recommendations-list [] (template/node [:ul#recommended-links]))

(defn add-tag-popup
  [popup-id link]
  (template/node
   [:div.modal.hide {:data-keyboard true :id popup-id}
    [:div.modal-header
     [:button.close {:data-dismiss "modal"} "x"]]
    [:div.row-fluid
     [:div.modal-body
      [:div.control-group.new-tag-cg
       [:div.controls
        [:div.span8.offset3
         [:input.new-tag {:type "text" :placeholder "tag"}]]
        [:div.span8.offset2 [:span.help-inline.hidden.new-tag-error]]]
       [:div.span8.offset4
        [:button.btn.btn-primary {:type "submit"} "Add tag"]]]]]]))

(defn tag-link
  ([tag] (tag-link tag false false))
  ([tag button? selected?]
     (let [[tag-link tag-text btn-s]
           (if tag
             [(str "#" tag) tag ".btn-primary"]
             ["#" "Remove tag filtering" ".btn-danger"])
           span (keyword
                 (str "span.tag-filter"
                      (when button? (str ".btn" btn-s tag-link
                                         (when selected? ".btn-success")))))]
       (template/node
        [:span [:a.set-tag! {:href tag-link} [span tag-text]]]))))

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
  [link tags title note bookmark-id popup-id]
  (template/node
   [:div.bookmark.well.well-small {:id bookmark-id}
    [:div [:a {:href link :target "_blank"} (or title link)]
     [:button.close.btn-danger.delete-bookmark! "Delete"]]
    [:textarea.note {:placeholder "Here you can place a note"} note]
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

(defn- add-tag-clicked
  [link n event]
  (let [tag (read-tag-to-add n)]
    (if (v/valid-tag? tag)
      (r/add-tag! tag link n)
      (r/new-tag-validation-failed n))))

;;; Render a bookmark.
(defn render-bookmark [{:keys [link new? tags title note]}]
  (let [id (s/generate-id link)
        popup-id (str "modal-" id)
        bookmark-id (str "bookmark-" id)
        n (bookmark-div link tags title note bookmark-id popup-id)]
    (ef/at js/document
           ["#bookmarks"]
           (let [adder (if new? ef/prepend ef/append)] (adder n))

           [(str "#" bookmark-id " .delete-bookmark!")]
           (events/listen :click (fn [event] (r/remove-bookmark! link n)))

           [(str "#" bookmark-id " .note")]
           (events/listen
            :change
            (fn [event] (let [note (read-note n)] (r/add-note! link note))))

           [(str "#" popup-id " .new-tag")]
           (ef/do->
             (generate-enter-up-listener (partial add-tag-clicked link n))
             (events/listen
             :input
             (fn [event]
               (let [tag (read-tag-to-add n)]
                 (if (or (empty? tag) (v/valid-tag? tag))
                   (r/new-tag-validation-succeeded {:node n})
                   (r/new-tag-validation-failed n))))))

           [(str "#" popup-id " .btn")]
           (events/listen
            :click
            (partial add-tag-clicked link n)))
    (when (not (empty? tags))
      (doall (map #(p/publish-tag (p/->TagAddedEvent n link % false)) tags)))))

(defn render-tag-menu-element [{:keys [tag reset-menu?]}]
  (let [selected? (and tag (s/tag-selected? tag))
        tag-node (tag-link tag true selected?)]
    (ef/at js/document
           ["#tags"]
           (ef/append tag-node " "))
    (ef/at tag-node
           [".set-tag!"]
           (events/listen
            :click
            (fn [event] (p/publish-tag-changed (p/->TagChangedEvent tag)))))))

(em/defaction render-recommendations-are-loading []
  ["#recs"]
  (ef/content recommendations-are-loading))

;;; TODO update list when it's exhausted
(defn render-recommendation
  [node link]
  (let [link-node (recommendation-node link)]
    (ef/at node (ef/append link-node))
    (ef/at link-node
           (events/listen
            :click
            (fn [event]
              (r/add-bookmark! link)
              (remove-node {:node link-node}) )))))

(defn render-recommendations-list
  [{:keys [links]}]
  (let [recs-list (recommendations-list)]
    (ef/at js/document
           ["#recs"]
           (ef/content recs-list))
    (doseq [link links] (render-recommendation recs-list link))))

;;; ## Event handlers

(defn- add-new-link-clicked
  [event]
  (let [link (read-link-to-add)
        link (if (and (not= (.indexOf link "http://") 0)
                      (not= (.indexOf link "https://") 0))
               (str "http://" link)
               link)]
    (if (v/valid-url? link)
      (r/add-bookmark! link)
      (r/new-link-validation-failed "Incorrect URL."))))

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-handlers []
  ["#link-to-add"]
  (generate-enter-up-listener add-new-link-clicked)
  ["#add-bookmark!"]
  (events/listen
   :click
   add-new-link-clicked)
  ["#recs-toggle"]
  (events/listen
   :click
   (fn [e]
     (when (classes/has (.-currentTarget e) "collapsed")
       (do (render-recommendations-are-loading)
           (r/get-recommendations))))))

(ns urdar.client.dom
  "Various functions that deal with representation of page."
  (:require [urdar.client.remote :as r]
            [urdar.client.signal :as s]
            [urdar.client.state :as st]
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

(defn read-query
  "Reads a current value of link in text field."
  []
  (string/trim
   (:link (ef/from js/document
                   :link ["#control-panel #search-input"]
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
  ([selector]
     (clear-input-element selector js/document))
  ([selector node]
     (ef/at node [selector] (ef/set-prop :value ""))))

(em/defaction remove-all-bookmarks []
  [".bookmark"] (ef/remove-node))

;;; TODO remove handler when deleting
(defn remove-node [node] (ef/at node (ef/remove-node)))

(defn remove-tag-node [node tag]
  (if (st/tag-selected? tag)
    (s/signal
     (s/->BookmarkRemovedSignal (get-parent (get-grandparent node)) false))
    (remove-node node)))

(defn clean-tags-menu [reset-menu?]
  (when reset-menu?
    (ef/at js/document
           ["#tags"]
           (ef/content ""))))

(defn unselect-tag-filters []
  (ef/at js/document [".tag-filter"] (ef/remove-class "btn-success")))

(em/defaction tag-filter-selected [tag]
  [".tag-filter"] (ef/remove-class "btn-success")
  [(str "#" (or tag "#all+bookmarks"))] (ef/add-class "btn-success"))

;;; ## Templates

(def recommendations-are-loading
  (template/node
   [:span
    [:i.icon-refresh.icon-spin.icon-large]
    "Please wait while recommendations are prepared..."]))

(defn recommendation-node
  [link title]
  (template/node [:li [:a {:href link :target "_blank"} (or title link)] " "
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
     (let [[tag-url tag-text]
           (if tag
             [(str "#" tag) tag]
             ["#all+bookmarks" "All bookmarks"])
           span (keyword
                 (str "span.tag-filter"
                      (when button? (str ".btn.input-block-level" tag-url
                                         (when selected? ".btn-success")))))]
       (template/node
        [(if button? :li :span)
         [:a.set-tag! {:href tag-url} [span tag-text]]]))))

(defn tag-element
  "Creates a HTML element for tag."
  [tag]
  (template/node
   [:span
    [:span.label (tag-link tag) " "
     [:a.remove-tag! {:href "#delete-tag"} [:i.icon-remove]]]
    " "]))

(defn note-template
  [note]
  (template/node [:span
                  [:button.btn.btn-small.edit-note! {:href "#edit-note"}
                   [:i.icon-edit] "Edit note"]
                  [:div.note note]]))

(defn edit-note-template
  [note]
  (template/node
   [:span
    [:button.btn.btn-small.save-note! [:i.icon-save] "Save changes"]
    [:textarea.note {:placeholder "Your notes goes here"} note]]))

(defn bookmark-div
  "Creates a bookmark HTML element."
  [link tags title note bookmark-id popup-id]
  (template/node
   [:div.bookmark.well.well-small {:id bookmark-id}
    [:div [:a {:href link :target "_blank"} (or title link)]
     [:button.close.btn-danger.delete-bookmark! "Delete"]]
    [:div [:span.note-holder]]
    [:div.tags [:i.icon-tags] "Tags: "
     [:a.add-tag! {:href "#add-tags" :data-toggle "modal"
                   :data-target (str "#" popup-id)}
      [:i.icon-plus]]
     " "
     [:span.tags-list]
     (add-tag-popup popup-id link)]]))

;;; ## Rendering

(em/defaction new-link-validation-succeeded []
  ["#add-bookmark-error"] (ef/add-class "hidden")
  ["#add-bookmark"] (ef/remove-class "error"))

(em/defaction new-link-validation-failed [error-msg]
  ["#add-bookmark"] (ef/add-class "error")
  ["#add-bookmark-error"] (ef/do-> (ef/content error-msg)
                                   (ef/remove-class "hidden")))

(defn new-tag-validation-succeeded [node]
  (ef/at node
   [".new-tag-cg"] (ef/remove-class "error")
   [".new-tag-error"] (ef/add-class "hidden")))

(defn new-tag-validation-failed
  [node error-msg]
  (let [error-msg (if (nil? error-msg)
                    (str "Tag should contain no more than 50 alphanumeric "
                         "characters, dashes  or underscores.")
                    error-msg)]
    (ef/at node
           [".new-tag-cg"] (ef/add-class "error")
           [".new-tag-error"] (ef/do-> (ef/content error-msg)
                                       (ef/remove-class "hidden")))))

(defn render-tag [node link tag]
  (let [tag-node (tag-element tag)]
    (ef/at node [".tags-list"] (ef/prepend tag-node))
    (ef/at tag-node
           [".remove-tag!"]
           (events/listen :click (fn [event] (r/remove-tag! tag link tag-node)))

           [".set-tag!"]
           (events/listen
            :click
            (fn [event]
              (st/set-search! nil)
              (s/signal (s/->TagFilterChangedSignal tag)))))))

(defn- add-tag-clicked
  [link n event]
  (let [tag (read-tag-to-add n)]
    (if (v/valid-tag? tag)
      (r/add-tag! tag link n)
      (s/signal (s/->NewTagValidationFailed n nil)))))

(defn render-note
  [node edit? link note]
  (let [n (if edit? (edit-note-template note) (note-template note))]
    (ef/at node
           [".note-holder"]
           (ef/content n))
    (if edit?
      (ef/at n
             [".save-note!"]
             (events/listen
              :click
              (fn [_]
                (s/signal
                 (s/->NoteChangeSignal node false true link (read-note n))))))
      (ef/at n
             [".edit-note!"]
             (events/listen
              :click
              (fn [_]
                (s/signal
                 (s/->NoteChangeSignal node true false link note))))))))

;;; Render a bookmark.
(defn render-bookmark [link new? tags title note]
  (let [id (st/generate-id link)
        popup-id (str "modal-" id)
        bookmark-id (str "bookmark-" id)
        n (bookmark-div link tags title note bookmark-id popup-id)]
    (ef/at js/document
           ["#bookmarks"]
           (let [adder (if new? ef/prepend ef/append)] (adder n))

           [(str "#" bookmark-id " .delete-bookmark!")]
           (events/listen :click (fn [event] (r/remove-bookmark! link n)))

           [(str "#" popup-id " .new-tag")]
           (ef/do->
             (generate-enter-up-listener (partial add-tag-clicked link n))
             (events/listen
             :input
             (fn [event]
               (let [tag (read-tag-to-add n)]
                 (if (or (empty? tag) (v/valid-tag? tag))
                   (new-tag-validation-succeeded n)
                   (s/signal (s/->NewTagValidationFailed n nil)))))))

           [(str "#" popup-id " .btn")]
           (events/listen
            :click
            (partial add-tag-clicked link n)))
    (s/signal (s/->NoteChangeSignal n false false link note))
    (when (not (empty? tags))
      (doall (map #(s/signal (s/->RenderTagSignal n link %)) tags)))))

(defn render-tag-menu-element [tag]
  (let [selected? (st/tag-selected? tag)
        tag-node (tag-link tag true selected?)]
    (ef/at js/document
           ["#tags"]
           (ef/append tag-node " "))
    (ef/at tag-node
           [".set-tag!"]
           (events/listen
            :click
            (fn [event]
              (st/set-query! nil)
              (s/signal (s/->TagFilterChangedSignal tag)))))))

(em/defaction render-recommendations-are-loading []
  ["#recs"]
  (ef/content recommendations-are-loading))

;;; TODO update list when it's exhausted
(defn render-recommendation
  [node link title]
  (let [link-node (recommendation-node link title)]
    (ef/at node (ef/append link-node))
    (ef/at link-node
           (events/listen
            :click
            (fn [event]
              (r/add-bookmark! link)
              (remove-node link-node))))))

(defn render-recommendations-list
  [links]
  (let [recs-list (recommendations-list)]
    (ef/at js/document
           ["#recs"]
           (ef/content recs-list))
    (doseq [{:keys [title url]} links]
      (render-recommendation recs-list url title))))

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
      (s/signal (s/->NewLinkValidationFailed "Incorrect URL.")))))

(defn- search-clicked
  [event]
  (let [query (read-query)]
    (s/signal (s/->SearchSignal query))))

(em/defaction add-handlers []
  ["#link-to-add"]
  (generate-enter-up-listener add-new-link-clicked)
  ["#add-bookmark!"]
  (events/listen :click add-new-link-clicked)
  ["#search-input"]
  (generate-enter-up-listener search-clicked)
  ["#search!"]
  (events/listen :click search-clicked)
  ["#recs-toggle"]
  (events/listen
   :click
   (fn [e]
     (when (classes/has (.-currentTarget e) "collapsed")
       (do (render-recommendations-are-loading)
           (r/get-recommendations))))))

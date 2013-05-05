(ns urdar.client.remote
  "Calls to a server."
  (:require [urdar.client.pubsub :as p]
            [urdar.client.state :as s]
            [cljs.reader :as r]
            [enfocus.core :as ef]
            [shoreleave.remotes.request :as remote])
  (:require-macros [enfocus.macros :as em]))

;;; TODO move to correct namespace, call using pubsub

;;; Adds a validation failed notification to a new link adder.
(em/defaction new-link-validation-failed [error-msg]
  ["#add-bookmark"] (ef/add-class "error")
  ["#add-bookmark-error"] (ef/do-> (ef/content error-msg)
                                   (ef/remove-class "hidden")))

;;; Removes a validation failed notification to a new link adder.
(em/defaction new-link-validation-succeeded [_]
  ["#add-bookmark-error"] (ef/add-class "hidden")
  ["#add-bookmark"] (ef/remove-class "error"))

(defn new-tag-validation-failed
  ([node]
     (new-tag-validation-failed node (str "Tag should contain no more than "
                                          "50 alphanumeric characters, "
                                          "dashes  or underscores.")))
  ([node error-msg]
     (ef/at node
            [".new-tag-cg"] (ef/add-class "error")
            [".new-tag-error"] (ef/do-> (ef/content error-msg)
                                        (ef/remove-class "hidden")))))

(defn new-tag-validation-succeeded [{:keys [node]}]
  (ef/at node
   [".new-tag-cg"] (ef/remove-class "error")
   [".new-tag-error"] (ef/add-class "hidden")))


;;;--------------------------------------------------------------------------

;;; TODO more elaborate error handling for some remotes

(defn fetch-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  ([]
     (let [{:keys [bookmarks-fetched bookmarks-to-fetch tag]} @s/state]
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
            (p/publish-bookmark (p/->BookmarkAddedEvent (:link b) false
                                               (:tags b))))))
      :on-error
      (fn [_] (ef/log-debug "Error while downloading bookmarks.")))))

(defn fetch-tags
  "Retrieves all currently existing tags of user from DB."
  [{:keys [update-tag-menu?] :or {update-tag-menu? true}}]
  (when update-tag-menu?
    (remote/request
     [:get "/_/tags"]
     :headers {"Content-Type" "application/edn;charset=utf-8"}
     :on-success
     (fn [{tags-str :body}]
       (let [tags (r/read-string tags-str)]
         (p/publish-tags-menu-change (p/->TagMenuChange nil true))
         (doseq [tag tags]
           (p/publish-tags-menu-change (p/->TagMenuChange tag false)))))
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
       (p/publish-bookmark (p/->BookmarkAddedEvent (:link b) true []))
       (p/publish-tag-changed (p/->TagChangedEvent nil))))
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
   (fn [_] (p/publish-bookmark-removed (p/->BookmarkRemovedEvent node true)))
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
   (fn [_] (p/publish-tag (p/->TagAddedEvent node link tag true)))
   :on-error
   (fn [{status :status}]
     (condp = status
       422 (new-tag-validation-failed node "Incorrect tag.")
       302 (new-tag-validation-failed node "Tag already exists.")))))

(defn remove-tag!
  "Untags a link for current user."
  [tag link node]
  (remote/request
   [:delete "/_/tags"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :tag tag})
   :on-success
   (fn [_] (p/publish-tag-removed (p/->TagRemovedEvent tag node)))
   :on-error
   (fn [_] (ef/log-debug "Error while deleting tag."))))

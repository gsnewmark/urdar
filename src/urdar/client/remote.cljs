(ns urdar.client.remote
  "Calls to a server."
  (:require [urdar.client.state :as st]
            [urdar.client.signal :as s]
            [cljs.reader :as r]
            [enfocus.core :as ef]
            [shoreleave.remotes.request :as remote])
  (:require-macros [enfocus.macros :as em]))

;;; TODO more elaborate error handling for some remotes

(defn fetch-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  ([]
     (let [{:keys [bookmarks-fetched bookmarks-to-fetch tag query]} @st/state]
       (fetch-bookmarks tag query bookmarks-fetched bookmarks-to-fetch)))
  ([tag query bookmarks-fetched bookmarks-to-fetch]
     (remote/request
      [:get
       (str "/_/bookmarks/" bookmarks-fetched "/"
            bookmarks-to-fetch "/"
            (if query
              (str (or tag "+") "/" query)
              tag))]
      :headers {"Content-Type" "application/edn;charset=utf-8"}
      :on-success
      (fn [{body :body}]
        (let [bookmarks (r/read-string body)]
          (doseq [b bookmarks]
            (s/signal
             (s/->BookmarkAddedSignal (:link b) false (:tags b)
                                      (:title b) (:note b))))))
      :on-error
      (fn [_] (ef/log-debug "Error while downloading bookmarks.")))))

(defn fetch-tags
  "Retrieves all currently existing tags of user from DB."
  [update-tag-menu?]
  (when update-tag-menu?
    (remote/request
     [:get "/_/tags"]
     :headers {"Content-Type" "application/edn;charset=utf-8"}
     :on-success
     (fn [{tags-str :body}]
       (let [tags (r/read-string tags-str)]
         (s/signal (s/->TagMenuChangedSignal nil true))
         (doseq [tag tags]
           (s/signal (s/->TagMenuChangedSignal tag false)))))
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
       (s/signal
        (s/->BookmarkAddedSignal (:link b) true (:tags b)
                                 (:title b) (:note b)))
       (s/signal (s/->TagFilterChangedSignal nil))))
   :on-error
   (fn [{status :status}]
     (condp = status
       409 (s/signal (s/->NewLinkValidationFailed "Bookmark already exists."))
       422 (s/signal (s/->NewLinkValidationFailed "Incorrect URL."))))))

(defn remove-bookmark!
  "Adds bookmark for current user in DB."
  [link node]
  (remote/request
   [:delete "/_/bookmarks"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link})
   :on-success
   (fn [_] (s/signal (s/->BookmarkRemovedSignal node true)))
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
   (fn [_] (s/signal (s/->TagAddedSignal node link tag true)))
   :on-error
   (fn [{status :status}]
     (condp = status
       422 (s/signal (s/->NewTagValidationFailed node "Incorrect tag."))
       302 (s/signal
            (s/->NewTagValidationFailed node "Tag already exists."))))))

(defn remove-tag!
  "Untags a link for current user."
  [tag link node]
  (remote/request
   [:delete "/_/tags"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :tag tag})
   :on-success
   (fn [_] (s/signal (s/->TagRemovedSignal tag node)))
   :on-error
   (fn [_] (ef/log-debug "Error while deleting tag."))))

(defn add-note!
  "Adds a note to link for current user."
  [link note]
  (remote/request
   [:post "/_/notes"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :content (pr-str {:link link :note note})
   :on-success
   (fn [_] (ef/log-debug "Note added successfully."))
   :on-error
   (fn [{status :status}] (ef/log-debug "Error while updating note."))))

(defn get-recommendations
  "Retrieves links recommended for current user."
  []
  (remote/request
   [:get "/_/recommendations"]
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :on-success
   (fn [{recommendations-str :body}]
     (let [recommendations (r/read-string recommendations-str)]
       (s/signal (s/->RecommendationReceivedSignal recommendations))))
   :on-error
   (fn [_] (ef/log-debug "Error while downloading recommendations."))))

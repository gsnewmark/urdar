(ns urdar.client.state)

;;; TODO bookmarks-to-fetch should be based on screen height
(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 20 :tag nil}))

(defn bookmark-fetched! [_]
  (swap! state update-in [:bookmarks-fetched] inc))

(defn bookmark-removed! [_]
  (swap! state update-in [:bookmarks-fetched] dec))

(defn set-tag! [{:keys [tag]}]
  ;; TODO should be one swap! call
  (swap! state assoc :tag tag)
  (swap! state assoc :bookmarks-fetched 0))

(defn generate-id [link] (:bookmarks-fetched @state))

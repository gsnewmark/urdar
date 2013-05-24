(ns urdar.client.state
  (:require [urdar.client.signal :as s]))

;;; TODO bookmarks-to-fetch should be based on screen height
(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 20 :tag nil
                  :bookmarks-id 0 :query nil}))

(defn bookmark-fetched! []
  (swap! state update-in [:bookmarks-fetched] inc))

(defn bookmark-removed! []
  (swap! state update-in [:bookmarks-fetched] dec))

(defn set-bookmarks-to-fetch [n]
  (swap! state assoc :bookmarks-to-fetch n))

(defn tag-selected? [tag] (= (:tag @state) tag))

(defn set-tag! [tag]
  ;; TODO should be one swap! call
  (swap! state assoc :tag tag)
  (swap! state assoc :bookmarks-fetched 0))

(defn set-query! [query]
  (swap! state assoc :query query))

(defn unset-tag! [tag]
  (let [{tag-selected :tag bookmarks-fetched :bookmarks-fetched} @state]
    (when (and (or (nil? tag) (and tag (= tag-selected tag)))
               (< bookmarks-fetched 1))
      (s/signal (s/->TagFilterChangedSignal nil)))))

(defn reset-id [] (swap! state assoc :bookmarks-id 0))

;;; TODO incorrect - will produce identical IDs when page is refreshed
(defn generate-id [link]
  (:bookmarks-id (swap! state update-in [:bookmarks-id] inc)))

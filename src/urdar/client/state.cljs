(ns urdar.client.state
  (:require [urdar.client.pubsub :as p]))

;;; TODO bookmarks-to-fetch should be based on screen height
(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 20 :tag nil}))

(defn bookmark-fetched! [_]
  (swap! state update-in [:bookmarks-fetched] inc))

(defn bookmark-removed! [_]
  (swap! state update-in [:bookmarks-fetched] dec))

(defn check-tag [tag] (= (:tag @state) tag))

(defn set-tag! [{:keys [tag]}]
  ;; TODO should be one swap! call
  (swap! state assoc :tag tag)
  (swap! state assoc :bookmarks-fetched 0))

(defn unset-tag! [{:keys [tag]}]
  (let [{tag-selected :tag bookmarks-fetched :bookmarks-fetched} @state]
    (when (and (or (nil? tag) (and tag (= tag-selected tag)))
               (< bookmarks-fetched 1))
      (p/publish-tag-changed (p/->TagChangedEvent nil)))))

(defn generate-id [link] (:bookmarks-fetched @state))

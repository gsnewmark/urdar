(ns urdar.client.main
  "Entry point of client-side code."
  (:require [urdar.client.dom :as d]
            [urdar.client.endless-scroll :as es]
            [urdar.client.pubsub :as p]
            [urdar.client.remote :as r]
            [urdar.client.state :as s]
            [shoreleave.brepl :as brepl]))

;;; TODO should publish event about bookmark removal, not delete them directly
(defn refetch-bookmarks [_]
  (d/remove-all-bookmarks)
  (r/fetch-bookmarks))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (p/subscribe-to-bookmarks s/bookmark-fetched!)
  (p/subscribe-to-bookmarks r/new-link-validation-succeeded)
  (p/subscribe-to-bookmarks d/render-bookmark)
  (p/subscribe-to-bookmarks-removed d/remove-node)
  (p/subscribe-to-bookmarks-removed s/bookmark-removed!)
  (p/subscribe-to-bookmarks-removed r/fetch-tags)
  (p/subscribe-to-tags-menu-changes d/clean-tags-menu)
  (p/subscribe-to-tags-menu-changes d/render-tag-menu-element)
  (p/subscribe-to-tags d/render-tag)
  (p/subscribe-to-tags r/fetch-tags)
  (p/subscribe-to-tags-removed d/remove-node)
  (p/subscribe-to-tags-removed r/fetch-tags)
  (p/subscribe-to-tag-changed s/set-tag!)
  (p/subscribe-to-tag-changed refetch-bookmarks)
  (d/add-new-link-click-handler)
  (r/fetch-tags {:update-tag-menu? true})
  (r/fetch-bookmarks)
  (set! (.-onscroll js/window) (es/generate-on-scroll r/fetch-bookmarks))
  (brepl/connect))

(set! (.-onload js/window) start)

(ns urdar.client.main
  "Entry point of client-side code."
  (:require [urdar.client.dom :as d]
            [urdar.client.endless-scroll :as es]
            [urdar.client.remote :as r]
            [urdar.client.state :as s]
            [urdar.client.reactions :as reactions]
            [shoreleave.brepl :as brepl]))

(defn items-to-fetch []
  (-> (es/document-height)
      (/ 350.0)
      js/Math.ceil
      (* 4)
      js/Math.floor
      inc))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (s/reset-id)
  (s/set-bookmarks-to-fetch (items-to-fetch))
  (reactions/activate-reactions)
  (d/add-handlers)
  (r/fetch-tags true)
  (r/fetch-bookmarks)
  ;; TODO find out why search in state is reseted
  (set! (.-onscroll js/window) (es/generate-on-scroll r/fetch-bookmarks))
  (set! (.-onresize js/window) #(s/set-bookmarks-to-fetch (items-to-fetch)))
  (brepl/connect))

(set! (.-onload js/window) start)

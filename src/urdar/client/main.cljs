(ns urdar.client.main
  "Entry point of client-side code."
  (:require [enfocus.core :as ef]
            [enfocus.events :as events]
            [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable])
  (:require-macros [enfocus.macros :as em]))

;;; ## PubSub-related utility variables/functions

(def ^{:private true} bus (pbus/bus))
(def bookmarks-topic (pubsub/topicify :bookmarks))
(def publish-bookmark (partial pubsub/publish bus bookmarks-topic))
(def subscribe-to-bookmarks (partial pubsub/subscribe bus bookmarks-topic))

;;; ## DOM handling/rendering code

(defn read-link-to-add
  "Reads a current value of link in text field."
  []
  (ef/from js/document
           :link ["#control-panel #add-bookmark #link-to-add"]
           (ef/get-prop :value)))

(defn render-bookmark
  "Render a bookmark."
  [bookmark]
  (ef/at js/document
         ["#bookmarks"]
         (ef/prepend bookmark)))

;;; ## Events

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-new-link-click-handler []
  ["#add-bookmark!"]
  (events/listen
   :click
   (fn [event]
     (let [link (:link (read-link-to-add))] (publish-bookmark link)))))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (subscribe-to-bookmarks render-bookmark)
  (add-new-link-click-handler))

(set! (.-onload js/window) start)

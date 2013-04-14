(ns urdar.client.main
  "Entry point of client-side code."
  (:require [cljs.reader :as r]
            [enfocus.core :as ef]
            [enfocus.events :as events]
            [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable]
            [shoreleave.remotes.request :as remote]
            [shoreleave.brepl :as brepl])
  (:require-macros [enfocus.macros :as em]))

;;; NOTE maybe store currently shown links in atom
;;;      and use it as a 'publisher'

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

;;; ## Interactions with server

;;; TODO publish according to date added
(defn get-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  []
  (remote/request
   [:get "/_/bookmarks"]
   :on-success (fn [{body :body}]
                 (let [bookmarks (r/read-string body)]
                   (doseq [b bookmarks] (publish-bookmark b))))))

(defn add-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:post "/_/add-bookmark"]
   :headers {"Content-Type" "application/edn"}
   :content (pr-str {:link link})
   :on-success (fn [{link :body}]
                 (publish-bookmark (r/read-string link)))))

;;; ## Events

;;; TODO validation

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-new-link-click-handler []
  ["#add-bookmark!"]
  (events/listen
   :click
   (fn [event]
     (let [link (:link (read-link-to-add))] (add-bookmark! link)))))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (subscribe-to-bookmarks render-bookmark)
  (add-new-link-click-handler)
  (brepl/connect)
  (get-bookmarks))

(set! (.-onload js/window) start)

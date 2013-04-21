(ns urdar.client.main
  "Entry point of client-side code."
  (:require [urdar.crossovers.validation :as v]
            [cljs.reader :as r]
            [clojure.string :as s]
            [enfocus.core :as ef]
            [enfocus.events :as events]
            [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable]
            [shoreleave.remotes.request :as remote]
            [dommy.template :as template]
            [shoreleave.brepl :as brepl])
  (:require-macros [enfocus.macros :as em]))

(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 10}))

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

;; TODO add http:// if bookmark doesn't have it
;; TODO buttons to delete link, add tags
;; TODO show title of page, not link itself
(defn bookmark-div
  "Creates a bookmark HTML element."
  [bookmark]
  (template/node
   [:div.bookmark.well.well-small
    [:a {:href bookmark :target "_blank"} bookmark]]))

(defn render-bookmark
  "Render a bookmark."
  [bookmark]
  (ef/at js/document
         ["#bookmarks"]
         (ef/prepend (bookmark-div bookmark))))

;;; Adds a validation failed notification to a new link adder.
(em/defaction new-link-validation-failed [error-msg]
  ["#add-bookmark"] (ef/add-class "error")
  ["#add-bookmark-error"] (ef/do-> (ef/content error-msg)
                                   (ef/remove-class "hidden")))

;;; Removes a validation failed notification to a new link adder.
(em/defaction new-link-validation-succeeded [_]
  ["#add-bookmark-error"] (ef/add-class "hidden")
  ["#add-bookmark"] (ef/remove-class "error"))

;;; ## Interactions with server

;;; TODO publish according to date added
(defn get-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  []
  (let [{:keys [bookmarks-fetched bookmarks-to-fetch]} @state]
    (remote/request
    [:get (str "/_/bookmarks/" bookmarks-fetched "/" bookmarks-to-fetch)]
    :headers {"Content-Type" "application/edn"}
    :on-success (fn [{body :body}]
                  (let [bookmarks (r/read-string body)]
                    (doseq [b (reverse bookmarks)]
                      (publish-bookmark (:link b))))))))

(defn add-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:post "/_/add-bookmark"]
   :headers {"Content-Type" "application/edn"}
   :content (pr-str {:link link})
   :on-success (fn [{link :body}]
                 (publish-bookmark (r/read-string link)))
   :on-error (fn [{status :status}]
               (condp = status
                 409 (new-link-validation-failed "Bookmark already exists.")
                 422 (new-link-validation-failed "Incorrect URL.")))))

;;; ## Events

;;; Publishes a newly added link when users clicks on the button.
(em/defaction add-new-link-click-handler []
  ["#add-bookmark!"]
  (events/listen
   :click
   (fn [event]
     (let [link (-> (:link (read-link-to-add))
                    s/trim)
           link (if (and (not= (.indexOf link "http://") 0)
                         (not= (.indexOf link "https://") 0))
                  (str "http://" link)
                  link)]
       (if (v/valid-url? link)
         (add-bookmark! link)
         (new-link-validation-failed "Incorrect URL."))))))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (subscribe-to-bookmarks render-bookmark)
  (subscribe-to-bookmarks new-link-validation-succeeded)
  (add-new-link-click-handler)
  (brepl/connect)
  (get-bookmarks))

(set! (.-onload js/window) start)

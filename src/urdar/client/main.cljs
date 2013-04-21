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
            [shoreleave.brepl :as brepl]
            [goog.async.Throttle])
  (:require-macros [enfocus.macros :as em]))

(def state (atom {:bookmarks-fetched 0 :bookmarks-to-fetch 10
                  :reached-bottom true}))

;;; ## State management

(defn bookmark-fetched [_]
  (swap! state update-in [:bookmarks-fetched] inc))

;;; ## PubSub-related utility variables/functions

(defrecord BookmarkEvent [link new?])

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

;; TODO buttons to delete link, add tags
;; TODO show title of page, not link itself
(defn bookmark-div
  "Creates a bookmark HTML element."
  [bookmark]
  (template/node
   [:div.bookmark.well.well-small
    [:a {:href bookmark :target "_blank"} bookmark]]))

;;; Render a bookmark.
(em/defaction render-bookmark [{:keys [link new?]}]
  ["#bookmarks"]
  (let [adder (if new? ef/prepend ef/append)]
    (adder (bookmark-div link))))

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

(defn fetch-bookmarks
  "Retrieves all currently existing bookmarks of user from DB. "
  ([]
     (let [{:keys [bookmarks-fetched bookmarks-to-fetch]} @state]
       (fetch-bookmarks bookmarks-fetched bookmarks-to-fetch)))
  ([bookmarks-fetched bookmarks-to-fetch]
     (remote/request
      [:get (str "/_/bookmarks/" bookmarks-fetched "/" bookmarks-to-fetch)]
      :headers {"Content-Type" "application/edn"}
      :on-success
      (fn [{body :body}]
        (let [bookmarks (r/read-string body)]
          (doseq [b bookmarks]
            (publish-bookmark (->BookmarkEvent (:link b) false))))))))

(defn add-bookmark!
  "Adds bookmark for current user in DB."
  [link]
  (remote/request
   [:post "/_/add-bookmark"]
   :headers {"Content-Type" "application/edn"}
   :content (pr-str {:link link})
   :on-success
   (fn [{link :body}]
     (publish-bookmark (->BookmarkEvent (r/read-string link) true)))
   :on-error
   (fn [{status :status}]
     (condp = status
       409 (new-link-validation-failed "Bookmark already exists.")
       422 (new-link-validation-failed "Incorrect URL.")))))

;;; ## Utils

(defn document-height
  "Retrieves height of document."
  []
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max (.-scrollHeight body) (.-offsetHeight body) (.-clientHeight html)
         (.-scrollHeight html) (.-offsetHeight html))))

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

;;; ## Endless Scroll

;;; TODO change throttle
(def throttle (goog.async.Throttle. fetch-bookmarks 2500))
(defn show-next-page []
  (.fire throttle))

;;; stop scrolling when end reached
(defn on-scroll []
  (let [max-height (document-height)
        scrolled (+ (.-pageYOffset js/window) (.-innerHeight js/window))
        left-to-scroll (- max-height scrolled)]
    (when (< left-to-scroll 5) (show-next-page))))

;;; ## Application starter

(defn ^:export start
  "Starts required listeners."
  []
  (subscribe-to-bookmarks render-bookmark)
  (subscribe-to-bookmarks new-link-validation-succeeded)
  (subscribe-to-bookmarks bookmark-fetched)
  (add-new-link-click-handler)
  (brepl/connect)
  (fetch-bookmarks)
  (set! (.-onscroll js/window) on-scroll))

(set! (.-onload js/window) start)

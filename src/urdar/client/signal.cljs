(ns urdar.client.signal
  (:require [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable]))

(def ^{:private true} bus (pbus/bus))
(def ^{:private true} signals-topic (pubsub/topicify :signals))

(def add-reactor (partial pubsub/subscribe bus signals-topic))

(def signal
  "Sends the given signal."
  (partial pubsub/publish bus signals-topic))

(defrecord BookmarkAddedSignal [link new? tags title note])
(defrecord BookmarkRemovedSignal [node update-tag-menu?])
(defrecord TagMenuChangedSignal [tag reset-menu?])
(defrecord TagAddedSignal [node link tag update-tag-menu?])
(defrecord RenderTagSignal [node link tag])
(defrecord TagRemovedSignal [tag node])
(defrecord TagFilterChangedSignal [tag])
(defrecord RecommendationReceivedSignal [links])
(defrecord NewLinkValidationFailed [msg])
(defrecord NewTagValidationFailed [node msg])
(defrecord NoteChangeSignal [node to-edit? save? link note])
(defrecord SearchSignal [query])

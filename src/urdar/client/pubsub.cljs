(ns urdar.client.pubsub
  "PubSub related bits: topics, functions to work with them as well as records
  for events."
  (:require [shoreleave.pubsubs.simple :as pbus]
            [shoreleave.pubsubs.protocols :as pubsub]
            [shoreleave.pubsubs.publishable]))

(defrecord BookmarkAddedEvent [link new? tags])
(defrecord BookmarkRemovedEvent [node])
(defrecord TagMenuChange [tag reset-menu?])
(defrecord TagAddedEvent [node link tag update-tag-menu?])
(defrecord TagRemovedEvent [node])
(defrecord TagChangedEvent [tag])

(def ^{:private true} bus (pbus/bus))

(def bookmarks-topic (pubsub/topicify :bookmarks))
(def publish-bookmark (partial pubsub/publish bus bookmarks-topic))
(def subscribe-to-bookmarks (partial pubsub/subscribe bus bookmarks-topic))

(def bookmarks-removed-topic (pubsub/topicify :bookmarks-removed))
(def publish-bookmark-removed
  (partial pubsub/publish bus bookmarks-removed-topic))
(def subscribe-to-bookmarks-removed
  (partial pubsub/subscribe bus bookmarks-removed-topic))

(def tags-menu-topic (pubsub/topicify :tags-menu))
(def publish-tags-menu-change (partial pubsub/publish bus tags-menu-topic))
(def subscribe-to-tags-menu-changes
  (partial pubsub/subscribe bus tags-menu-topic))

(def tags-topic (pubsub/topicify :tags))
(def publish-tag (partial pubsub/publish bus tags-topic))
(def subscribe-to-tags (partial pubsub/subscribe bus tags-topic))

(def tags-removed-topic (pubsub/topicify :tags-removed))
(def publish-tag-removed
  (partial pubsub/publish bus tags-removed-topic))
(def subscribe-to-tags-removed
  (partial pubsub/subscribe bus tags-removed-topic))

(def tag-changed-topic (pubsub/topicify :change-tag))
(def publish-tag-changed (partial pubsub/publish bus tag-changed-topic))
(def subscribe-to-tag-changed
  (partial pubsub/subscribe bus tag-changed-topic))

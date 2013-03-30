;; TODO remove code repetition
(ns urdar.datastore.neo4j
  "Operations with a Neo4j-based datastore."
  (:require [urdar.config :as config]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

;; ## Neo4j

(defn init-connection
  "Initiate connection to Neo4j REST API."
  [url login password]
  (nr/connect! url login password))

(comment
  (let [{:keys [url login password]} (:neo4j config/config)]
   (init-connection url login password)))

;; Create index for users.
(defonce users-index (nn/create-index "users"))
;; Create index for tags.
(defonce tags-index (nn/create-index "tags"))
;; Create index for bookmarks.
(defonce bookmarks-index (nn/create-index "bookmarks"))

;; TODO maybe use hashes for indices' keys

(defn get-user
  "Retrieves user node with given e-mail from given index."
  [index e-mail]
  (nn/find-one (:name index) "e-mail" e-mail))

(defn gen-utag
  "Generates a key used for tags index."
  [e-mail tag]
  (str e-mail "+" tag))

(defn get-tag
  "Retrieves tag node with given name for given e-mail from given index."
  [index e-mail tag-name]
  (nn/find-one (:name index) "utag" (gen-utag e-mail tag-name)))

(defn get-tags-for-user
  "Retrieves all tags' names of a given user node."
  [user-node]
  ;; TODO maybe shouldn't be lazy
  ;; TODO maybe return tag nodes, not names
  (map
   #(get % "tag.name")
   (cy/tquery "START user=node({sid}) MATCH user-[:has]->tag RETURN tag.name"
              {:sid (:id user-node)})))

(defn get-bookmark
  "Retrieves bookmark node with given link for given e-mail from given
index."
  [index link]
  (nn/find-one (:name index) "bookmark" link))

(defn get-bookmarks-for-user
  "Retrieves all bookmarks' links which a given user added."
  [user-node]
  ;; TODO maybe shouldn't be lazy
  ;; TODO maybe return tag nodes, not names
  (map
   #(get % "bookmark.link")
   (cy/tquery (str "START user=node({sid}) MATCH user-[:bookmarked]->bookmark"
                   " RETURN bookmark.link")
              {:sid (:id user-node)})))

(defn get-bookmarks-for-tag
  "Retrieves all bookmarks' links which given tag contains."
  [tag-node]
  ;; TODO maybe shouldn't be lazy
  ;; TODO maybe return tag nodes, not names
  (map
   #(get % "bookmark.link")
   (cy/tquery (str "START tag=node({sid}) MATCH tag-[:contains]->bookmark"
                   " RETURN bookmark.link")
              {:sid (:id tag-node)})))

(defn create-user
  "Creates a user as a node in graph and adds it to given index.
Returns nil if something goes wrong during creation."
  [index e-mail]
  (if-let [user (get-user index e-mail)]
    user
    (some-> (nn/create {:e-mail e-mail})
            :id
            (nn/add-to-index (:name index) "e-mail" e-mail true))))

(defn create-tag
  "Creates a tag as a node in graph for a given user node and adds it
to given index. Returns nil if something goes wrong during creation."
  [index user-node tag]
  (if-let [e-mail (get-in user-node [:data :e-mail])]
    (if-let [tag-node (get-tag index e-mail tag)]
      tag-node
      (some-> (nn/create {:name tag})
              :id
              (nn/add-to-index (:name index) "utag"
                               (gen-utag e-mail tag) true)
              (as-> t
                    (let [tag-node t]
                      (nrl/create user-node tag-node :has)
                      tag-node))))))

(defn create-bookmark
  "Creates a bookmark as a node in graph for a given user node and adds it to
given index. Returns nil if something goes wrong during creation."
  [index user-node link]
  (if-let [e-mail (get-in user-node [:data :e-mail])]
    (if-let [bookmark-node (get-bookmark index link)]
      bookmark-node
      (some-> (nn/create {:link link})
              :id
              (nn/add-to-index (:name index) "bookmark" link true)
              (as-> b
                    (let [bookmark-node b]
                      (nrl/create user-node bookmark-node :bookmarked
                                  {:on (pr-str (Date.))})
                      bookmark-node))))))

(defn tag-bookmark
  "Adds a given bookmark to a given tag."
  [tag-node bookmark-node]
  (nrl/create tag-node bookmark-node :contains))

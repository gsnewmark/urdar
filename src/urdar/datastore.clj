;; TODO fix (to work with new nj api)
(ns urdar.datastore
  "Operations with a datastore."
  (:require [urdar.datastore.neo4j :as nj]))

(defprotocol UserDatastore
  (user? [self e-mail]
    "Checks whether the user with given e-mail exists in datastore.")
  (get-user [self e-mail] "Retrieves user with given e-mail from datastore.")
  (create-user [self e-mail] "Creates user with given e-mail in datastore."))

(defprotocol TagDatastore
  (tag-exists? [self tag]
    "Checks whether the given tag exists in datastore.")
  (get-tag [self tag] "Retrieves given tag from datastore.")
  (create-tag [self tag] "Creates user with given e-mail in datastore."))

;; ## Datastore Neo4j-backed implementation

(defrecord Neo4jDatastore []
  UserDatastore
  (user? [_ e-mail]
    (not (nil? (nj/get-user e-mail nj/users-index))))
  (get-user [_ e-mail]
    (nj/get-user e-mail nj/users-index))
  (create-user [_ e-mail]
    (nj/create-user e-mail nj/users-root nj/users-index))

  TagDatastore
  (tag-exists? [_ tag]
    (not (nil? (nj/get-tag tag nj/tags-index))))
  (get-tag [_ tag]
    (nj/get-tag tag nj/tags-index))
  (create-tag [_ tag]
    (nj/create-tag tag nj/tags-root nj/tags-index)))

(def neo4j-datastore (->Neo4jDatastore))

;; TODO make a part of configuration
(def user-ds
  "Particular implementation of UserDatastore protocol."
  neo4j-datastore)

(def tag-ds
  "Particular implementation of TagDatastore protocol."
  neo4j-datastore)

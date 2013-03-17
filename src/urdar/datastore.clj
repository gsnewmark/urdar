(ns urdar.datastore
  "Operations with a datastore."
  (:require [urdar.datastore.neo4j :as nj]))

(defprotocol UserDatastore
  (user? [self e-mail]
    "Checks whether the user with given e-mail exists in datastore.")
  (get-user [self e-mail] "Retrieves user with given e-mail from datastore.")
  (create-user [self e-mail] "Creates user with given e-mail in datastore."))

;; ## UserDatastore Neo4j-backed implementation

(defrecord Neo4jUserDatastore []
  UserDatastore
  (user? [_ e-mail]
    (not (nil? (nj/get-user e-mail nj/users-index))))
  (get-user [_ e-mail]
    (nj/get-user e-mail nj/users-index))
  (create-user [_ e-mail]
    (nj/create-user e-mail nj/users-root nj/users-index)))

;; TODO make a part of configuration
(def user-ds
  "Particular implementation of UserDatastore protocol."
  (->Neo4jUserDatastore))

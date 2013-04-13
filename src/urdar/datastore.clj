(ns urdar.datastore
  "Operations with a datastore."
  (:require [urdar.config :as config]
            [urdar.datastore.neo4j :as nj]))

;;; TODO ability to remove tags/bookmarks/~users

(defprotocol Datastore
  (init [self]
    "Performs required initialization of datastore. Returns itself.")
  (user? [self e-mail]
    "Checks whether the user with given e-mail exists.")
  (create-user [self e-mail] "Creates user with given e-mail.")
  (tag-exists? [self e-mail tag]
    "Checks whether given tag exists for a given user.")
  (get-tags [self e-mail] "Retrieves all tags of given user.")
  (create-tag [self e-mail tag]
    "Creates tag for given user.")
  (link-exists? [self link] "Checks whether given link already added.")
  (create-bookmark [self e-mail link]
    "Creates a link and bookmarks it for a given user.")
  (tag-bookmark [self e-mail tag link] "Adds tag to given link.")
  (get-bookmarks [self e-mail] "Retrieves all bookmarks of given user.")
  (get-tagged-bookmarks [self e-mail tag]
    "Retrieves all bookmarks of given user which has given tag."))

;; ## Datastore Neo4j-backed implementation

(defrecord Neo4jDatastore []
  Datastore
  (init [self]
    (let [{:keys [url login password]} (:neo4j config/config)]
      (nj/init-connection url login password)
      self))
  (user? [_ e-mail]
    (not (nil? (nj/get-user-node nj/users-index e-mail))))
  (create-user [_ e-mail]
    (nj/get-or-create-user-node nj/users-index e-mail))
  (tag-exists? [_ e-mail tag]
    (not (nil? (nj/get-tag-node nj/tags-index e-mail tag))))
  (get-tags [self e-mail]
    (nj/get-tags-for-user (nj/get-user-node nj/users-index e-mail)))
  (create-tag [_ e-mail tag]
    (nj/get-or-create-tag-node
     nj/tags-index
     (nj/get-user-node nj/users-index e-mail)
     tag))
  (link-exists? [_ link]
    (not (nil? (nj/get-link-node nj/links-index link))))
  (create-bookmark [_ e-mail link]
    (nj/bookmark-link-node
     (nj/get-user-node nj/users-index e-mail)
     (nj/get-or-create-link-node nj/links-index link)))
  (tag-bookmark [_ e-mail tag link]
    (nj/tag-bookmark-node
     (nj/get-tag-node nj/tags-index e-mail tag)
     (nj/get-link-node nj/links-index link)))
  (get-bookmarks [_ e-mail]
    (nj/get-bookmarks-for-user (nj/get-user-node nj/users-index e-mail)))
  (get-tagged-bookmarks [_ e-mail tag]
    (nj/get-bookmarks-for-tag (nj/get-tag-node nj/tags-index e-mail tag))))

(def datastore
  "Interface to a data store."
  (-> (->Neo4jDatastore)
      (init)))

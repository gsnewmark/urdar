(ns urdar.datastore
  "Operations with a datastore."
  (:require [urdar.config :as config]
            [urdar.datastore.neo4j :as nj]))

;;; TODO ability to remove tags/bookmarks/~users
;;; TODO get-tagged-bookmarks should return Bookmark

(defrecord Bookmark [e-mail link date])

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
  (bookmark-exists? [self e-mail link]
    "Checks whether given link is already bookmarked by user.")
  (create-bookmark [self e-mail link]
    "Creates a link and bookmarks it for a given user. Returns Bookmark
     instance.")
  (tag-bookmark [self e-mail tag link] "Adds tag to given link.")
  (get-bookmarks [self e-mail]
    "Retrieves all bookmarks (instances of Bookmark) of given user sorted
     by their creation date (descending).")
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
  (bookmark-exists? [_ e-mail link]
    (not (nil? (nj/get-bookmark (nj/get-user-node nj/users-index e-mail)
                                (nj/get-link-node nj/links-index link)))))
  (create-bookmark [_ e-mail link]
    (if-let [bookmark
             (nj/bookmark-link-node
              (nj/get-user-node nj/users-index e-mail)
              (nj/get-or-create-link-node nj/links-index link))]
      (map->Bookmark bookmark)))
  (tag-bookmark [_ e-mail tag link]
    (nj/tag-bookmark-node
     (nj/get-tag-node nj/tags-index e-mail tag)
     (nj/get-link-node nj/links-index link)))
  (get-bookmarks [_ e-mail]
    (letfn [(cypher-res->Bookmark [res]
              (->Bookmark e-mail (get res "bookmark.link") (get res "r.on")))]
      (map cypher-res->Bookmark
           (nj/get-bookmarks-for-user
            (nj/get-user-node nj/users-index e-mail)))))
  (get-tagged-bookmarks [_ e-mail tag]
    (nj/get-bookmarks-for-tag (nj/get-tag-node nj/tags-index e-mail tag))))

(def datastore
  "Interface to a data store."
  (-> (->Neo4jDatastore)
      (init)))

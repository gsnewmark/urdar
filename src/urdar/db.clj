(ns urdar.db
  (:require [urdar.db.neo4j :as n4j]
            [urdar.config :as c]))

(defrecord User [e-mail date-signed])
(defrecord Bookmark [e-mail link title note tags date-added])

(defprotocol UserOperations
  "DB operations that could be performed during ordinary user's usage of
  application."
  (init-connection [self]
    "Performs required initialization of database connection and returns it.")
  (user-registered? [self e-mail]
    "Checks whether the user with given e-mail exists in database.")
  (register-user [self e-mail]
    "Creates user with given e-mail in DB. Returns instance of User.")
  (unregister-user [self e-mail]
    "Deletes user with given e-mail along with all theirs stored bookmarks
    from database.")
  (get-user [self e-mail] "Retrieves User instance if user is registered.")
  (bookmark-exists? [self e-mail link]
    "Checks whether given link is already bookmarked by user.")
  (add-bookmark [self e-mail link]
    "Bookmarks a given link (probably, creating it in DB) for the given user.
    Returns Bookmark instance.")
  (delete-bookmark [self e-mail link]
    "Deletes given link from user's bookmarks (if it's present). If no other
    bookmark is tagged by some tags of this bookmark, remove these tags as
    well.")
  (get-bookmark [self e-mail link]
    "Retrieves instance of Bookmark if user already saved it.")
  (get-bookmarks [self e-mail] [self e-mail skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) of
    given user sorted by their creation date (descending), optionally
    skipping first skip bookmarks.")
  (update-bookmark [self e-mail link & {:keys [title description]}]
    "Adds a title and/or description to a given bookmark. Returns instance
    of Bookmark.")
  (bookmark-tagged? [self e-mail link tag]
    "Checks whether given bookmark has given tag.")
  (tag-bookmark [self e-mail link tag]
    "Adds tag to given bookmark. Returns instance of Bookmark.")
  (untag-bookmark [self e-mail link tag]
    "Removes tag from given bookmark. If no other bookmark is tagged by this
    tag, also removes tag itself.")
  (get-tagged-bookmarks [self e-mail tag] [self e-mail tag skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) with
    given tag of given user sorted by their creation date (descending),
    optionally skipping first skip bookmarks.")
  (tag-exists? [self e-mail tag]
    "Checks whether given tag exists for a given user.")
  (get-tags [self e-mail] "Returns a set of all tags of given user."))

;;; TODO maybe use constructor from map
(defn- node->User
  [node]
  (when-let [{:keys [e-mail date-signed]} (:data node)]
    (->User e-mail date-signed)))

(defn- node->Bookmark
  [e-mail link tags node]
  (when-let [{:keys [title note date-added]} (:data node)]
    (->Bookmark e-mail link title note tags date-added)))

(defrecord UserOperationsNeo4j [url login password]
  UserOperations
  (init-connection [self] (n4j/init-connection url login password) self)
  (user-registered? [self e-mail] (not (nil? (n4j/get-user-node e-mail))))
  (register-user [self e-mail]
    (when-let [node (n4j/create-user-node e-mail)] (node->User node)))
  (unregister-user [self e-mail]
    (when-let [node (n4j/get-user-node e-mail)] (n4j/delete-user-node node)))
  (get-user [self e-mail]
    (when-let [node (n4j/get-user-node e-mail)] (node->User node)))
  (bookmark-exists? [self e-mail link]
    (not (nil? (n4j/get-bookmark-node (n4j/generate-key e-mail link)))))
  (add-bookmark [self e-mail link]
    (let [user-node (n4j/get-user-node e-mail)
          link-node (or (n4j/get-link-node link)
                        (n4j/create-link-node link))]
      (when (and user-node link-node)
        (some->> (n4j/create-bookmark-node user-node link-node)
                 (node->Bookmark e-mail link [])))))
  (delete-bookmark [self e-mail link]
    (when-let [node (n4j/get-bookmark-node (n4j/generate-key e-mail link))]
      (n4j/delete-bookmark-node node)))
  (get-bookmark [self e-mail link]
    (when-let [node (n4j/get-bookmark-node (n4j/generate-key e-mail link))]
      ;; TODO tags
      (node->Bookmark e-mail link [] node)))
  (get-bookmarks [self e-mail])
  (get-bookmarks [self e-mail skip quant])
  (update-bookmark [self e-mail link & {:keys [title description]}])
  (bookmark-tagged? [self e-mail link tag])
  (tag-bookmark [self e-mail link tag])
  (untag-bookmark [self e-mail link tag])
  (get-tagged-bookmarks [self e-mail tag])
  (get-tagged-bookmarks [self e-mail tag skip quant])
  (tag-exists? [self e-mail tag])
  (get-tags [self e-mail]))

(def u
  (let [{:keys [url login password]} (:neo4j c/config)]
    (-> (->UserOperationsNeo4j url login password)
        init-connection)))

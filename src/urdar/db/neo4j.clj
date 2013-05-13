(ns urdar.db.neo4j
  "Low-level operations with Neo4j database."
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

(defn init-connection
  [url login password]
  (nr/connect! url login password))

;;; ## Indices

(defonce users-index (nn/create-index "users"))
(defonce links-index (nn/create-index "links"))
(defonce tags-index (nn/create-index "tags"))
(defonce bookmarks-index (nn/create-index "bookmarks"))

(defn- get-from-index [index k v] (nn/find-one (:name index) k v))

;;; TODO find way to make it private once again
(defn generate-key [e-mail e] (str e-mail "+" e))

(defn get-user-node
  "Retrieves user node with given e-mail from given (or default) index."
  ([e-mail] (get-user-node users-index e-mail))
  ([index e-mail] (get-from-index index "e-mail" e-mail)))

(defn get-link-node
  "Retrieves link node with given url from given (or default) index."
  ([link] (get-link-node links-index link))
  ([index link] (get-from-index index "link" link)))

(defn get-tag-node
  "Retrieves tag node with given name from given (or default) index."
  ([e-mail tag] (get-tag-node tags-index e-mail tag))
  ([index e-mail tag] (get-from-index index "tag" (generate-key e-mail tag))))

(defn get-bookmark-node
  "Retrieves bookmark node from given (or default) index."
  ([v] (get-bookmark-node bookmarks-index v))
  ([index v] (get-from-index index "bookmark" v)))

;;; ## Entity creation

(defn- create-and-index
  "Unless node already exists (checked using one-argument predicate exists?,
  creates a node with given data and adds it to a given index using the given
  key and value pair."
  [index k v exists? data]
  (when-not (exists? v)
    (nn/create-unique-in-index (:name index) k v data)))

(defn create-user-node
  "Creates a user as a node in graph and adds it to given (or default) index.
  Returns nil if user already exists or created node otherwise."
  ([e-mail] (create-user-node users-index e-mail))
  ([index e-mail]
     (create-and-index index "e-mail" e-mail (partial get-user-node index)
                       {:e-mail e-mail
                        :date-signed (pr-str (Date.))
                        :type "user"})))

(defn create-link-node
  "Creates a link as a node in graph and adds it to given (or default) index.
  Returns nil if link already exists or created node otherwise."
  ([link] (create-link-node links-index link))
  ([index link]
     (create-and-index index "link" link (partial get-link-node index)
                       {:url link :type "link"})))

;;; TODO nil check
(defn create-bookmark-node
  "Creates a bookmark as a node in graph and adds it to given (or default)
  index. Additionally creates a connection between a bookmark and a user as
  well as between a bookmark and a link. Returns nil if bookmark already
  exists or created node otherwise."
  ([user-node link-node]
     (create-bookmark-node bookmarks-index user-node link-node))
  ([index user-node link-node]
     (let [e-mail (get-in user-node [:data :e-mail])
           link (get-in link-node [:data :url])
           bookmark-node (create-and-index index "bookmark"
                                           (generate-key e-mail link)
                                           (partial get-bookmark-node index)
                                           {:date-added (Date.)
                                            :type "bookmark"})]
       (nrl/maybe-create user-node bookmark-node :has)
       (nrl/maybe-create bookmark-node link-node :bookmarks)
       bookmark-node)))

;;; ## Entity deletion

(defn- delete-node
  "Deletes all relations with this node and then deletes the node itself."
  [node]
  (nrl/purge-all node)
  (nn/delete node))

(defn delete-user-node
  [user-node]
  ;; TODO delete all user's tags
  ;; TODO delete all user's bookmarks
  (delete-node user-node))

(defn delete-bookmark-node
  [bookmark-node]
  ;; TODO remove unused tags
  (delete-node bookmark-node))

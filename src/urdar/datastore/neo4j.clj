(ns urdar.datastore.neo4j
  "Operations with a Neo4j-based datastore."
  (:require [urdar.config :as config]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

;; ## Helpers

;; TODO probably change this
(defn init-connection
  "Initiate connection to Neo4j REST API."
  [url login password]
  (nr/connect! url login password)
  ;; Create index for users.
  (defonce users-index (nn/create-index "users"))
  ;; Create index for tags.
  (defonce tags-index (nn/create-index "tags"))
  ;; Create index for links.
  (defonce links-index (nn/create-index "links")))

(comment (let [{:keys [url login password]} (:neo4j config/config)]
   (init-connection url login password)))

(defn gen-utag
  "Generates a key used for tags index."
  [e-mail tag]
  (str e-mail "+" tag))

(defn run-query-from-root
  "Retrieves information from query that runs from a given root."
  ([root query] (run-query-from-root root query nil))
  ([root query field]
     (when root
       (let [res (cy/tquery query {:sid (:id root)})]
         (if field (map #(get % field) res) res)))))

(defn get-user-node
  "Retrieves user node with given e-mail from given index."
  [index e-mail]
  (nn/find-one (:name index) "e-mail" e-mail))

(defn get-tag-node
  "Retrieves tag node with given name for given e-mail from given index."
  [index e-mail tag-name]
  (nn/find-one (:name index) "utag" (gen-utag e-mail tag-name)))

(defn get-link-node
  "Retrieves bookmark node with given link for given e-mail from given
index."
  [index link]
  (nn/find-one (:name index) "link" link))

;; ## User info retrieval

(defn get-tags-for-user
  "Retrieves all tags' names of a given user node."
  [user-node]
  ;; TODO maybe shouldn't be lazy
  (run-query-from-root
   user-node
   "START user=node({sid}) MATCH user-[:has]->tag RETURN tag.name"
   "tag.name"))

(defn get-bookmarks-for-user
  "Retrieves all bookmarks or quant number of bookmarks skipping first skip
   bookmarks which a given user added."
  ([user-node] (get-bookmarks-for-user user-node nil nil))
  ([user-node skip quant]
     (run-query-from-root
      user-node
      (str "START user=node({sid}) MATCH user-[r:bookmarked]->bookmark "
           "RETURN bookmark.link, r.on ORDER BY r.on DESC "
           (when (and skip quant) (str "SKIP " skip " LIMIT " quant))))))

(defn get-bookmarks-for-tag
  "Retrieves all bookmarks' links which given tag contains."
  [tag-node]
  (run-query-from-root
   tag-node
   (str "START tag=node({sid}) MATCH tag-[:contains]->bookmark "
        "RETURN bookmark.link")
   "bookmark.link"))

;; ## Entity creation

(defn get-or-create-user-node
  "Creates a user as a node in graph and adds it to given index.
Returns nil if something goes wrong during creation."
  [index e-mail]
  (if-let [user (get-user-node index e-mail)]
    user
    (some-> (nn/create {:e-mail e-mail})
            :id
            (nn/add-to-index (:name index) "e-mail" e-mail true))))

(defn get-or-create-tag-node
  "Creates a tag as a node in graph for a given user node and adds it
to given index. Returns nil if something goes wrong during creation."
  [index user-node tag]
  (if-let [e-mail (get-in user-node [:data :e-mail])]
    (if-let [tag-node (get-tag-node index e-mail tag)]
      tag-node
      (some-> (nn/create {:name tag})
              :id
              (nn/add-to-index (:name index) "utag"
                               (gen-utag e-mail tag) true)
              (as-> t
                    (let [tag-node t]
                      (nrl/create user-node tag-node :has)
                      tag-node))))))

(defn get-or-create-link-node
  "Retrieves or creates a link."
  [index link]
  (or (get-link-node index link)
      (some-> (nn/create {:link link})
              :id
              (nn/add-to-index (:name index) "link" link true))))

;; ## Bookmark handling

(defn get-bookmark
  [user-node link-node]
  "Retrieves a :bookmark relation between given user and link."
  (when (and user-node link-node)
    (nrl/first-outgoing-between user-node link-node [:bookmarked])))

;;; TODO delete all tags of bookmark
(defn delete-bookmark
  [bookmark-node]
  "Deletes a given bookmark node."
  (nrl/maybe-delete (:id bookmark-node)))

(defn get-tagged
  "Retrieves a :contains relation between given tag and node."
  [tag-node link-node]
  (when (and tag-node link-node)
    (nrl/first-outgoing-between tag-node link-node [:contains])))

(defn bookmark-link-node
  "Bookmarks a given link by given user. Returns nil if bookmark already
   exists."
  [user-node link-node]
  (when (and user-node link-node (nil? (get-bookmark user-node link-node)))
    (let [bookmark-rel
          (nrl/create user-node link-node :bookmarked {:on (pr-str (Date.))})]
      (-> link-node
          :data
          (assoc :e-mail (get-in user-node [:data :e-mail])
                 :date (get-in bookmark-rel [:data :on]))))))

(defn tag-bookmark-node
  "Adds a given link to a given tag. Returns nil if bookmark already has
   given tag."
  [tag-node link-node]
  ;; TODO check if already exists
  (when (and tag-node link-node (nil? (get-tagged tag-node link-node)))
    (nrl/maybe-create tag-node link-node :contains)
    link-node))

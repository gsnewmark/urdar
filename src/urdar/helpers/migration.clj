(ns urdar.helpers.migration
  (:require [urdar.config :as config]
            [urdar.db :as db]
            [urdar.search :as s]
            [urdar.helpers.external-api :as e]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(def ^{:private true}
  bookmarks-search-index-v2 {:name "bookmarks-index" :type "bookmark"})

(defn- init-connection
  "Initiate connection to Neo4j REST API."
  []
  (let [{:keys [url login password]} (:neo4j config/config)
        {es-url :url} (:es config/config)]
    (nr/connect! url login password)
    (def users-index (nn/create-index "users"))
    (def tags-index (nn/create-index "tags"))
    (def links-index (nn/create-index "links"))
    (def links-index-v2 (nn/create-index "linksIndex"))
    (def users-index-v2 (nn/create-index "usersIndex"))
    (esr/connect! es-url)
    (when-not (esi/exists? (:name bookmarks-search-index-v2))
       (let [mapping-types
             {(:type bookmarks-search-index-v2)
              {:properties {:e-mail {:type "string" :index "not_analyzed"}
                            :title {:type "string" :analyzer "standard"
                                    :boost 2.0}
                            :link {:type "string" :index "not_analyzed"}
                            :note {:type "string" :analyzer "standard"}}}}]
         (esi/create (:name bookmarks-search-index-v2)
                     :mappings mapping-types)))))

(defn update-node
  [node data]
  (nn/update node (merge (:data node) data)))

(defn- retrieve-users-v1
  [index]
  (cy/tquery (str "START user=node:users(\"e-mail:*\") RETURN user")))

(defn- retrieve-bookmarks-for-user-v1
  [index e-mail]
  (cy/tquery (str "START user=node:" (:name index) "({key}={value}) "
                  "MATCH (user)-[r:bookmarked]->(bookmark), "
                  "(user)-[?:has]->(tag)-[:contains]->(bookmark) "
                  "RETURN bookmark.link, COLLECT(DISTINCT tag.name?), r.on "
                  "ORDER BY r.on")
             {:key "e-mail" :value e-mail}))

(defn- recreate-data-v1->v2
  [old-index]
  (let [old-users (map #(get % "user") (retrieve-users-v1 old-index))]
    (doseq [u old-users]
      (let [e-mail (get-in u [:data :e-mail])
            bookmarks (retrieve-bookmarks-for-user-v1 old-index e-mail)]
        (db/register-user e-mail)
        (doseq [b bookmarks]
          (let [{tags "COLLECT(DISTINCT tag.name?)"
                 link "bookmark.link"} b]
            (db/add-bookmark e-mail link)
            (doseq [tag tags] (db/tag-bookmark e-mail link tag))))))))

(defn- retrieve-links-v2
  [index]
  (cy/tquery (str "START link=node:linksIndex(\"link:*\") RETURN link")))

(defn- update-links-titles-v2
  [index]
  (let [links (retrieve-links-v2 index)]
    (doseq [l (map #(get % "link") links)]
      (when-not (get-in l [:data :title])
        (let [url (get-in l [:data :url])
              title (e/retrieve-title url)]
          (when title
            (update-node (nn/find-one (:name index) "link" url)
                         {:title title})))))))

(defn- retrieve-users-v2
  [index]
  (cy/tquery (str "START user=node:" (or (:name index) "usersIndex")
                  "(\"e-mail:*\") RETURN user.`e-mail`")))

(defn- retrieve-bookmarks-for-user-v2
  [index e-mail]
  (cy/tquery (str "START user=node:" (or (:name index) "usersIndex")
                  "({key}={value}) "
                  "MATCH (user)-[:has]->(b)-[:bookmarks]->(l) "
                  "RETURN l.title?, b.note?, l.url")
             {:key "e-mail" :value e-mail}))

(defn- add-bookmark-to-search-index
  [u-index si-name si-type]
  (let [e-mails (map #(get % "user.e-mail") (retrieve-users-v2 u-index))]
    (doseq [e-mail e-mails]
      (let [bookmarks (retrieve-bookmarks-for-user-v2 u-index e-mail)]
        (doseq [b bookmarks]
          (let [{link "l.url" title "l.title?" note "b.note?"} b
                doc (s/->BookmarkDoc e-mail link title note)]
            (esd/create si-name si-type doc)))))))

(defn migrate-v1-v2
  []
  (init-connection)
  (recreate-data-v1->v2 users-index))

(defn update-titles-v2
  []
  (init-connection)
  (update-links-titles-v2 links-index-v2))

(defn add-to-search-index-v2
  []
  (init-connection)
  (add-bookmark-to-search-index users-index-v2
                                (:name bookmarks-search-index-v2)
                                (:type bookmarks-search-index-v2)))

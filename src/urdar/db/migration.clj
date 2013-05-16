(ns urdar.db.migration
  (:require [urdar.config :as config]
            [urdar.db :as db]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

(defn- init-connection
  "Initiate connection to Neo4j REST API."
  []
  (let [{:keys [url login password]} (:neo4j config/config)]
    (nr/connect! url login password)
    (def users-index (nn/create-index "users"))
    (def tags-index (nn/create-index "tags"))
    (def links-index (nn/create-index "links"))))

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
  (let [old-users (map #(get % "user") (retrieve-users old-index))]
    (doseq [u old-users]
      (let [e-mail (get-in u [:data :e-mail])
            bookmarks (retrieve-bookmarks-for-user old-index e-mail)]
        (db/register-user e-mail)
        (doseq [b bookmarks]
          (let [{tags "COLLECT(DISTINCT tag.name?)"
                 link "bookmark.link"} b]
            (db/add-bookmark e-mail link)
            (doseq [tag tags] (db/tag-bookmark e-mail link tag))))))))

(defn migrate-v1->v2
  []
  (init-connection)
  (recreate-data-v1->v2 users-index))

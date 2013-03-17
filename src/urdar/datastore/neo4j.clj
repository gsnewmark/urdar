(ns urdar.datastore.neo4j
  "Operations with a Neo4j-based datastore."
  (:require [urdar.config :as config]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

;; ## Neo4j

;; Initiate connection to Neo4j REST API.
(let [{:keys [url login password]} (:neo4j config/config)]
  (nr/connect! url login password))

;; Create index for graphs roots.
(defonce roots-index (nn/create-index "roots"))
;; Create index for users.
(defonce users-index (nn/create-index "users"))

;; Root of users graph.
(defonce users-root (nn/find-one (:name roots-index) "root" "user"))

(defn get-user
  "Retrieves user with given e-mail from given index."
  [e-mail index]
  (nn/find-one (:name index) "e-mail" e-mail))

(defn create-user
  "Creates a user as a node in graph with given root and adds it in index."
  [e-mail root index]
  (let [user (get-user e-mail index)]
    (if-not user
      (if-let [user (nn/create {:e-mail e-mail})]
        (do
          (some-> user
                  :id
                  (nn/add-to-index (:name index) "e-mail" e-mail true)
                  (as-> user
                        (nrl/create root user :registered
                                    {:date (pr-str (Date.))})))
          user))
      user)))

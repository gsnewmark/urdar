(ns urdar.db.neo4j
  "Low-level operations with Neo4j database."
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy])
  (:import [java.util Date]))

;;; ## Indices

(defn init-connection
  [url login password]
  (nr/connect! url login password)
  (def users-index-impl (nn/create-index "usersIndex"))
  (def users-index (with-meta users-index-impl {:key "e-mail"}))
  (def links-index-impl (nn/create-index "linksIndex"))
  (def links-index (with-meta links-index-impl {:key "link"}))
  (def tags-index-impl (nrl/create-index "tagsIndex"))
  (def tags-index (with-meta tags-index-impl {:key "tag"}))
  (def bookmarks-index-impl (nn/create-index "bookmarksIndex"))
  (def bookmarks-index (with-meta bookmarks-index-impl {:key "bookmark"})))

(defn- number-of-entries-in-index
  ([index] (number-of-entries-in-index index ""))
  ([index default-key]
     (let [key (or (:key (meta index)) default-key)]
       (get (first (cy/tquery (str "START n=node:" (:name index)
                                   "(\"" key ":*\") "
                                   "RETURN COUNT(n)")))
            "COUNT(n)"))))

(defn- get-from-index
  [index k v]
  (let [key (or (:key (meta index)) k)]
    (nn/find-one (:name index) key v)))

(defn- generate-key [e-mail & e] (apply str e-mail "+" (interpose "+" e)))

(defn get-user-node
  "Retrieves user node with given e-mail from given (or default) index."
  ([e-mail] (get-user-node users-index e-mail))
  ([index e-mail] (get-from-index index "e-mail" e-mail)))

(defn get-link-node
  "Retrieves link node with given url from given (or default) index."
  ([link] (get-link-node links-index link))
  ([index link] (get-from-index index "link" link)))

(defn get-tag-rel
  "Retrieves tag relationship with given name from given (or default) index."
  ([e-mail link tag-name] (get-tag-rel tags-index e-mail link tag-name))
  ([index e-mail link tag-name]
     (let [key (or (:key (meta index)) "tag")]
       (nrl/find-one (:name index) key (generate-key e-mail link tag-name)))))

(defn- get-bookmark-node-impl [index v] (get-from-index index "bookmark" v))

(defn get-bookmark-node
  "Retrieves bookmark node from given (or default) index."
  ([e-mail link] (get-bookmark-node bookmarks-index e-mail link))
  ([index e-mail link]
     (get-bookmark-node-impl index (generate-key e-mail link))))

;;; ## Entity creation

(defn- create-and-index
  "Unless node already exists (checked using one-argument predicate exists?,
  creates a node with given data and adds it to a given index using the given
  key and value pair."
  [index k v exists? data]
  (when-not (exists? v)
    (let [key (or (:key (meta index)) k)]
      (nn/create-unique-in-index (:name index) k v data))))

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
  ([link title] (create-link-node links-index link title))
  ([index link title]
     (create-and-index index "link" link (partial get-link-node index)
                       (merge {:url link :type "link"}
                              (if title {:title title} {})))))

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

           bookmark-node
           (create-and-index index "bookmark" (generate-key e-mail link)
                             (partial get-bookmark-node-impl index)
                             {:date-added (Date.) :type "bookmark"})]
       (nrl/maybe-create user-node bookmark-node :has)
       (nrl/maybe-create bookmark-node link-node :bookmarks)
       bookmark-node)))

;;; ## Entity update

(defn update-node
  [node data]
  (nn/update node (merge (:data node) data)))

;;; ## Bookmark operations

(defn tag-bookmark
  "Adds a tag relationship between given user and bookmark nodes."
  ([user-node bookmark-node link tag]
     (tag-bookmark tags-index user-node bookmark-node link tag))
  ([index user-node bookmark-node link tag]
     (let [key (or (:key (meta index)) "tag")
           e-mail (get-in user-node [:data :e-mail])]
       (nrl/create-unique-in-index user-node bookmark-node :tagged
                                   (:name index) key
                                   (generate-key e-mail link tag)
                                   {:tag tag}))))

(defn untag-bookmark
  "Removes a tag relationship."
  ([tag-rel] (untag-bookmark tags-index tag-rel))
  ([index tag-rel]
     (nrl/delete-from-index tag-rel (:name index))
     (nrl/delete tag-rel)))

;;; TODO order by?
;;; TODO return nodes, not list of strings?
(defn get-tags-for-bookmark
  "Retrieves list of all tags for bookmark."
  ([e-mail link] (get-tags-for-bookmark bookmarks-index e-mail link))
  ([index e-mail link]
     (map #(get % "t.tag")
          (cy/tquery (str "START b=node:" (:name index) "({key}={value}) "
                          "MATCH ()-[t:tagged]->(b) "
                          "RETURN DISTINCT t.tag")
                     {:key (or (:key (meta index)) "bookmark")
                      :value (generate-key e-mail link)}))))

(defn get-tags-for-user
  "Retrieves list of all tags for user"
  ([e-mail] (get-tags-for-user users-index e-mail))
  ([index e-mail]
     (map #(get % "t.tag")
          (cy/tquery (str "START u=node:" (:name index) "({key}={value}) "
                          "MATCH u-[t:tagged]->() "
                          "RETURN DISTINCT t.tag")
                     {:key (or (:key (meta index)) "e-mail") :value e-mail}))))

(defn get-tagged-bookmarks-for-user
  "Retrieves link, title, note, tags and date added for quant bookmarks
  (ordered by date added descending - newest first) of user starting from
  skip one."
  ([e-mail tag]
     (get-tagged-bookmarks-for-user users-index e-mail tag [nil nil]))
  ([e-mail tag skip-quant-vec]
     (get-tagged-bookmarks-for-user users-index e-mail tag skip-quant-vec))
  ([index e-mail tag [skip quant]]
     ;; TODO remove concatenation?
     (cy/tquery (str "START user=node:" (:name index) "({key}={value}) "
                     "MATCH (user)-[tag:tagged]->(b)-[:bookmarks]->(l), "
                     "(user)-[t:tagged]->(b) "
                     "WHERE tag.tag={tag} "
                     "RETURN b.`date-added`, l.title?, b.note?, l.url, "
                     "COLLECT(DISTINCT t.tag?) "
                     "ORDER BY `b.date-added` DESC"
                     (when (and skip quant)
                       (str " SKIP " skip " LIMIT " quant)))
                {:key (or (:key (meta index)) "e-mail") :value e-mail
                 :tag tag})))

(defn get-bookmarks-for-user
  "Retrieves link, title, note, tags and date added for quant bookmarks
  (ordered by date added descending - newest first) of user starting from
  skip one."
  ([e-mail] (get-bookmarks-for-user users-index e-mail [nil nil]))
  ([e-mail skip-quant-vec]
     (get-bookmarks-for-user users-index e-mail skip-quant-vec))
  ([index e-mail [skip quant]]
     ;; TODO remove concatenation?
     (cy/tquery (str "START user=node:" (:name index) "({key}={value}) "
                     "MATCH (user)-[:has]->(b)-[:bookmarks]->(l), "
                     "(user)-[t?:tagged]->(b) "
                     "RETURN b.`date-added`, l.title?, b.note?, l.url, "
                     "COLLECT(DISTINCT t.tag?) "
                     "ORDER BY `b.date-added` DESC"
                     (when (and skip quant)
                       (str " SKIP " skip " LIMIT " quant)))
                {:key (or (:key (meta index)) "e-mail") :value e-mail})))

(defn recommended-bookmarks-for-user
  "Finds n (or lesser) links that user hasn't yet bookmarked, but might be
  interested in."
  ([e-mail] (recommended-bookmarks-for-user 10 e-mail))
  ([n e-mail] (recommended-bookmarks-for-user users-index n e-mail))
  ([index n e-mail]
     (cy/tquery (str "START user=node:" (:name index) "({key}={value}) "
                     "MATCH (user)-[:has]->()-[:bookmarks]->(shared_link)"
                     "<-[:bookmarks]-()<-[:has]-(other)-[:has]->()"
                     "-[:bookmarks]->(other_link) "
                     "WHERE NOT(user-[:has]->()-[:bookmarks]->(other_link)) "
                     "WITH other.`e-mail` AS mail, "
                     "COUNT(shared_link) AS slc, other_link.url AS url, "
                     "other_link.title? AS title "
                     "ORDER BY slc DESC "
                     "RETURN url, COUNT(url) as cnt, title "
                     "ORDER BY cnt DESC"
                     (when n " LIMIT {n}"))
                {:key (or (:key (meta index)) "e-mail") :value e-mail
                 :n n})))

(defn random-bookmarks-for-user
  "Finds n (or lesser) random links that user hasn't yet bookmarked."
  ([e-mail] (random-bookmarks-for-user 10 e-mail))
  ([n e-mail] (random-bookmarks-for-user users-index n e-mail))
  ([u-index n e-mail]
     (random-bookmarks-for-user links-index u-index n e-mail))
  ([l-index u-index n e-mail]
     (let [lkey (or (:key (meta l-index)) "link") lvalue "*"
           links-count (number-of-entries-in-index l-index lkey)
           random-links-limit (* n 2)
           to-skip (if (> links-count random-links-limit)
                     (rand-int (- links-count random-links-limit))
                     0)]
       (cy/tquery
        (str "START "
             "link=node:" (:name l-index) "({query}), "
             "user=node:" (:name u-index) "({key}={value}) "
             "WITH link AS l, user AS u "
             "SKIP {toskip} LIMIT {rndlim} "
             " WHERE NOT((u)-[:has]->()-[:bookmarks]->(l)) "
             "RETURN l.url, l.title? "
             "LIMIT {n}")
        {:key (or (:key (meta u-index)) "e-mail") :value e-mail
         :n n :toskip to-skip :rndlim random-links-limit
         :query (str lkey ":" lvalue)}))))

(defn get-link-title
  ([link] (get-link-title links-index link))
  ([index link]
     (when-let [link-node (get-link-node index link)]
       (get-in link-node [:data :title]))))

;;; ## Entity deletion

(defn- delete-node
  "Deletes all relations with this node and then deletes the node itself."
  [index node]
  (nn/delete-from-index node (:name index))
  (nrl/purge-all node)
  (nn/delete node))

(defn delete-bookmark-node
  ([e-mail link bookmark-node]
     (delete-bookmark-node bookmarks-index tags-index
                           e-mail link bookmark-node))
  ([b-index t-index e-mail link bookmark-node]
     (let [tags (get-tags-for-bookmark b-index e-mail link)]
       (doseq [t tags]
         (untag-bookmark t-index (get-tag-rel t-index e-mail link t))))
     (delete-node b-index bookmark-node)))

(defn delete-user-node
  ([user-node] (delete-user-node users-index user-node))
  ([u-index user-node]
     (delete-user-node u-index bookmarks-index tags-index user-node))
  ([u-index b-index t-index user-node]
     (let [e-mail (get-in user-node [:data :e-mail])
           links (map #(get % "l.url")
                      (get-bookmarks-for-user u-index e-mail [nil nil]))

           bookmark-nodes
           (map (partial get-bookmark-node b-index e-mail) links)]
       (doseq [[l n] (partition 2 (interleave links bookmark-nodes))]
         (delete-bookmark-node b-index t-index e-mail l n))
       (delete-node u-index user-node))))

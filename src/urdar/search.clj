(ns urdar.search
  (:require [urdar.config :as c]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]))

(def bookmarks-index {:name "bookmarks-index" :type "bookmark"})

(defn init-connection
  "Connects to the ElasticSearch endpoint and creates an search index for
  bookmarks information if it is not yet created."
  ([] (init-connection (get-in c/config [:es :url])))
  ([url]
     (esr/connect! url)
     (when-not (esi/exists? (:name bookmarks-index))
       (let [mapping-types
             {(:type bookmarks-index)
              {:properties {:e-mail {:type "string" :index "not_analyzed"}
                            :title {:type "string" :analyzer "standard"
                                    :boost 2.0}
                            :link {:type "string" :index "not_analyzed"}
                            :note {:type "string" :analyzer "standard"}}}}]
         (esi/create (:name bookmarks-index) :mappings mapping-types)))))

(defrecord BookmarkDoc [e-mail link title note])

(defn index-bookmark
  "Adds the given bookmark information document to the search index."
  ([bookmark-doc]
     (index-bookmark bookmarks-index bookmark-doc))
  ([index bookmark-doc]
     (let [{:keys [type name]} index]
       (esd/create name type bookmark-doc))))

(defn find-bookmark-id
  "Finds an ID in search index of the given bookmark."
  ([e-mail link] (find-bookmark-id bookmarks-index e-mail link))
  ([index e-mail link]
     (let [res  (esd/search (:name index) (:type index)
                            :filter {:and [{:term {:e-mail e-mail}}
                                           {:term {:link link}}]})
           hit (first (esrsp/hits-from res))]
       (:_id hit))))

(defn unindex-all-bookmarks
  "Removes all bookmarks of the given user (specified by e-mail) from the
  search index."
  ([e-mail] (unindex-all-bookmarks bookmarks-index e-mail))
  ([index e-mail]
     (esd/delete-by-query (:name index) (:type index)
                          {:filtered {:query (q/match-all)
                                      :filter {:term {:e-mail e-mail}}}})))

(defn unindex-bookmark
  "Removes the given bookmark (specified by user's e-mail and link) from
  search index."
  ([e-mail link] (unindex-bookmark bookmarks-index e-mail link))
  ([index e-mail link]
     (when-let [id (find-bookmark-id index e-mail link)]
       (esd/delete (:name index) (:type index) id))))

(defn update-bookmark
  "Updates bookmark stored in the search index with new information."
  ([e-mail link bookmark-doc]
     (update-bookmark bookmarks-index e-mail link bookmark-doc))
  ([index e-mail link bookmark-doc]
     (let [id (find-bookmark-id e-mail link)]
       (esd/put (:name index) (:type index) id bookmark-doc))))

(defn- es-result->map
  [es-result]
  (get-in es-result [:_source :link]))

(defn find-bookmarks
  "Searches for bookmarks of the given user (specified by e-mail) that
  satisfies the given query (simple text string). Returns `quant` results
  starting from `from`."
  ([from quant e-mail query]
     (find-bookmarks bookmarks-index from quant e-mail query))
  ([index from quant e-mail query]
     ;; TODO find way to return only :link in query itself
     (let [res  (esd/search (:name index) (:type index)
                            :query (q/match "_all" query)
                            :filter {:term {:e-mail e-mail}}
                            :from from :size quant)
           hits (esrsp/hits-from res)]
       (map es-result->map hits))))

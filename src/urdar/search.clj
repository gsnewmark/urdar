(ns urdar.search
  (:require [urdar.config :as c]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]))

(def bookmarks-index {:name "bookmarks-index-3" :type "bookmark"})

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
                            :link {:type "string" :index "not_analyzed"
                                   :boost 2.0}
                            :note {:type "string" :analyzer "standard"}}}}]
         (esi/create (:name bookmarks-index) :mappings mapping-types)))))

(defn create-bookmark-doc
  "Creates a bookmark information document (map) with the given attributes."
  [e-mail link title note]
  {:e-mail e-mail :link link :title title :note note})

(defn index-bookmark
  "Adds the given bookmark information document to the search index."
  ([bookmark-doc]
     (index-bookmark bookmarks-index bookmark-doc))
  ([index bookmark-doc]
     (let [{:keys [type name]} index]
       (println (esd/create name type bookmark-doc)))))

(comment  ;;; TODO
  (defn unindex-bookmark)

;;; TODO
  (defn update-bookmark))

(defn- es-result->map
  [es-result]
  (let [info (:_source es-result)]
    (select-keys info [:link :title])))

(defn find-bookmarks
  "Searches for bookmarks of the given user (specified by e-mail) that
  satisfies the given query (simple text string). Returns `quant` results
  starting from `from`."
  [index from quant e-mail query]
  (let [res  (esd/search (:name index) (:type index)
                         ;:query (q/match-all)
                         :query (q/match "_all" query)
                         :filter {:term {:e-mail e-mail}}
                         :from from :size quant)
        hits (esrsp/hits-from res)]
    (map es-result->map hits)))

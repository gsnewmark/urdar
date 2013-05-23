(ns urdar.controllers.api
  (:require [urdar.db :as db]
            [urdar.search :as s]
            [urdar.crossovers.validation :as v]
            [urdar.helpers.external-api :as e]))

;;; TODO throw exceptions instead of edn-responses, create middleware
;;;      to transform them into response

(defn edn-response [& {:keys [body status]}]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :body (when body (pr-str body))})

(defn str-integer? [s] (not (nil? (when s (re-matches #"\d+" s)))))

(defn get-bookmarks [e-mail skip quant]
  (if (and e-mail (str-integer? skip) (str-integer? quant))
    (let [bookmarks (db/get-bookmarks e-mail skip quant)]
     (edn-response :body (doall (map (partial into {}) bookmarks))))
    (edn-response :status 422)))

(defn get-tags [e-mail]
  (if e-mail
    (edn-response :body (db/get-tags e-mail))
    (edn-response :status 422)))

(defn get-tagged-bookmarks [e-mail tag skip quant]
  (if (and e-mail tag (str-integer? skip) (str-integer? quant))
    (edn-response :body
                  (doall (map (partial into {})
                              (db/get-tagged-bookmarks e-mail tag skip quant))))
    (edn-response :status 422)))

;;; TODO search + tag filtering
(defn search-bookmarks [e-mail query skip quant]
  (if (and e-mail query (str-integer? skip) (str-integer? quant))
    (let [search-results (s/find-bookmarks skip quant e-mail query)
          bookmarks (map (partial db/get-bookmark e-mail) search-results)]
      (edn-response :body (doall (map (partial into {}) bookmarks))))
    (edn-response :status 422)))

(defn add-bookmark! [e-mail link]
  (if-not (and e-mail link (v/valid-url? link))
    (edn-response :status 422)
    (if (db/bookmark-exists? e-mail link)
      (edn-response :status 409)
      (let [title (or (db/get-title link) (e/retrieve-title link))
            bookmark (db/add-bookmark e-mail link title)
            bookmark-doc (-> (dissoc bookmark :tags :date-added)
                             (assoc :e-mail e-mail :title title)
                             (s/map->BookmarkDoc))]
        (s/index-bookmark bookmark-doc)
        (edn-response :body (into {} bookmark))))))

(defn delete-bookmark! [e-mail link]
  (if-not (and e-mail link (v/valid-url? link))
    (edn-response :status 422)
    (do ((juxt s/unindex-bookmark db/delete-bookmark) e-mail link)
        (edn-response :status 204))))

(defn add-tag! [e-mail tag link]
  (if-not (and e-mail tag link (v/valid-tag? tag))
    (edn-response :status 422)
    (if (not (nil? (db/tag-bookmark e-mail link tag)))
      (edn-response :status 204 :body {:tag tag})
      (edn-response :status 302))))

(defn remove-tag! [e-mail tag link]
  (if-not (and e-mail tag link)
    (edn-response :status 422)
    (do (db/untag-bookmark e-mail link tag)
        (edn-response :status 204))))

(defn add-note! [e-mail link note]
  (if-not (and e-mail link note)
    (edn-response :status 422)
    (do (db/update-bookmark e-mail link note)
        (s/update-bookmark
         e-mail link
         (s/->BookmarkDoc e-mail link (db/get-title link) note))
        (edn-response :status 204))))

(defn get-recommendations [e-mail]
  (if-not e-mail
    (edn-response :status 422)
    (edn-response :body (db/recommend-bookmarks e-mail))))

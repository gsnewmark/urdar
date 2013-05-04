(ns urdar.controllers.api
  (:require [urdar.datastore :as ds]
            [urdar.crossovers.validation :as v]))

;;; TODO throw exceptions instead of edn-responses, create middleware
;;;      to transform them into response

(defn edn-response [& {:keys [body status]}]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn;charset=utf-8"}
   :body (when body (pr-str body))})

(defn str-integer? [s] (not (nil? (when s (re-matches #"\d+" s)))))

(defn get-bookmarks [e-mail skip quant]
  (if (and e-mail (str-integer? skip) (str-integer? quant))
    (let [bookmarks (ds/get-bookmarks ds/datastore e-mail skip quant)]
     (edn-response :body (doall (map (partial into {}) bookmarks))))
    (edn-response :status 422)))

(defn get-tags [e-mail]
  (if e-mail
    (edn-response :body (ds/get-tags ds/datastore e-mail))
    (edn-response :status 422)))

(defn get-tagged-bookmarks [e-mail tag skip quant]
  (if (and e-mail tag (str-integer? skip) (str-integer? quant))
    (edn-response :body
                  (doall (map (partial into {})
                              (ds/get-tagged-bookmarks ds/datastore
                                                       e-mail tag skip quant))))
    (edn-response :status 422)))

(defn add-bookmark! [e-mail link]
  (if-not (and e-mail link (v/valid-url? link))
    (edn-response :status 422)
    (if (ds/bookmark-exists? ds/datastore e-mail link)
      (edn-response :status 409)
      (edn-response
       :body (into {} (ds/create-bookmark ds/datastore e-mail link))))))

(defn delete-bookmark! [e-mail link]
  (if-not (and e-mail link (v/valid-url? link))
    (edn-response :status 422)
    (do (ds/delete-bookmark ds/datastore e-mail link)
        (edn-response :status 204))))

(defn add-tag! [e-mail tag link]
  (if-not (and e-mail tag link (v/valid-tag? tag))
    (edn-response :status 422)
    (if (not (nil? (ds/tag-bookmark ds/datastore e-mail tag link)))
      (edn-response :status 204 :body {:tag tag})
      (edn-response :status 302))))

(defn remove-tag! [e-mail tag link]
  (if-not (and e-mail tag link)
    (edn-response :status 422)
    (do (ds/untag-bookmark ds/datastore e-mail tag link)
        (edn-response :status 204))))

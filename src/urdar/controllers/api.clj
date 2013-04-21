(ns urdar.controllers.api
  (:require [urdar.datastore :as ds]
            [urdar.crossovers.validation :as v]))

(defn edn-response [& {:keys [body status]}]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (when body (pr-str body))})

;;; TODO add date when bookmark was added

(defn get-bookmarks [e-mail]
  (if e-mail
    (edn-response
     :body (doall (map (partial into {}) (ds/get-bookmarks ds/datastore e-mail))))
    (edn-response :status 422)))

(defn add-bookmark! [e-mail link]
  (if-not (and e-mail link (v/valid-url? link))
    (edn-response :status 422)
    (if (ds/bookmark-exists? ds/datastore e-mail link)
      (edn-response :status 409)
      (do (ds/create-bookmark ds/datastore e-mail link)
          (edn-response :body link)))))

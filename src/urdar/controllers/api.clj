(ns urdar.controllers.api
  (:require [urdar.datastore :as ds]))

(defn edn-response [& {:keys [body status]}]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (when body (pr-str body))})

;;; TODO proper exception messages (when no e-mail), all exception cases

(defn get-bookmarks [e-mail]
  (if e-mail
    (edn-response :body (ds/get-bookmarks ds/datastore e-mail))
    (edn-response :status 422)))

(defn add-bookmark [e-mail link]
  (if (and e-mail link)
    (do (ds/create-bookmark ds/datastore e-mail link)
        (edn-response :body link)))
  (edn-response :status 422))

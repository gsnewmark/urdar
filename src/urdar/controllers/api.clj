(ns urdar.controllers.api
  (:require [urdar.datastore :as ds]))

;;; TODO move extraction of e-mail to utils
;;; TODO add response codes

(defn get-bookmarks [{session :session}]
  (if-let [e-mail (:e-mail session)]
    (ds/get-bookmarks ds/datastore e-mail)))

(defn add-bookmark [{session :session params :params}]
  (if-let [e-mail (:e-mail session)]
    (ds/create-bookmark ds/datastore e-mail (:link params))))

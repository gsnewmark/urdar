(ns urdar.controllers.site
  (:require [urdar.config :as c]
            [urdar.datastore :as ds]
            [urdar.views :as views]
            [urdar.helpers.external-api :as api]
            [cemerick.friend :as friend]
            [ring.util.response :as rr]))

(defn- register-user
  [e-mail]
  (let [link (get-in c/config [:new-user-first-entry :link])
        tag (get-in c/config [:new-user-first-entry :tag])]
    (println e-mail link tag)
    (ds/create-user ds/datastore e-mail)
    (ds/create-bookmark ds/datastore e-mail link)
    (ds/tag-bookmark ds/datastore e-mail tag link)))

(defn index [{session :session :as request}]
  ;; TODO shouldn't overwrite session on each request
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    (let [e-mail (or (:e-mail session) (api/get-user-mail-address request))
          session (assoc session :e-mail e-mail)]
      (when-not (ds/user? ds/datastore e-mail)
        (register-user e-mail))
      (-> (rr/response (views/index e-mail))
          (assoc :session session)))
    views/index-unregistered))

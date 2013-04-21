(ns urdar.controllers.site
  (:require [urdar.datastore :as ds]
            [urdar.views :as views]
            [urdar.helpers.external-api :as api]
            [cemerick.friend :as friend]
            [ring.util.response :as rr]))

(defn index [{session :session :as request}]
  ;; TODO shouldn't overwrite session on each request
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    (let [e-mail (or (:e-mail session) (api/get-user-mail-address request))
          session (assoc session :e-mail e-mail)]
      (when-not (ds/user? ds/datastore e-mail)
        (ds/create-user ds/datastore e-mail))
      (-> (rr/response (views/index e-mail))
          (assoc :session session)))
    views/index-unregistered))

(ns urdar.controllers.site
  "Contains logic for routes."
  (:require [urdar.datastore :as ds]
            [urdar.views :as views]
            [urdar.helpers.external-api :as api]
            [cemerick.friend :as friend]))

(defn index [request]
  (let [e-mail (api/get-user-mail-address request)]
    (when-not (ds/user? ds/user-ds e-mail)
      (ds/create-user ds/user-ds e-mail))
    (views/index e-mail)))

(defn login
  "Redirects to index page in case user is already logged in."
  [request]
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    {:status 302 :headers {"Location" "/"}}
    views/login))

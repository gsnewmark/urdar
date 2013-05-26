(ns urdar.controllers.site
  (:require [urdar.config :as c]
            [urdar.db :as db]
            [urdar.views :as views]
            [urdar.helpers.external-api :as api]
            [cemerick.friend :as friend]
            [ring.util.response :as rr]))

(defn- register-user
  [e-mail]
  (let [link (get-in c/config [:new-user-first-entry :link])
        tag (get-in c/config [:new-user-first-entry :tag])]
    (db/register-user e-mail)
    (db/add-bookmark e-mail link "Urdar Source Code")))

(defn index [{session :session :as request}]
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    (let [e-mail (or (:e-mail session) (api/get-user-mail-address request))
          session (assoc session :e-mail e-mail)]
      (when-not (db/user-registered?  e-mail)
        (register-user e-mail))
      (-> (rr/response (views/index e-mail))
          (assoc :session session)))
    views/index-unregistered))

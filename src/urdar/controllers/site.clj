(ns urdar.controllers.site
  "Contains logic for routes."
  (:require [urdar.views :as views]
            [urdar.helpers.requests :as api]
            [urdar.helpers.utils :as u]
            [cemerick.friend :as friend]))

(defn index [request]
  (let [access-token (u/get-access-token request)]
    (views/index (api/github-get access-token ""))))

(defn login
  "Redirects to index page in case user is already logged in."
  [request]
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    {:status 302 :headers {"Location" "/"}}
    views/login))

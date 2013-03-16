(ns urdar.controllers.site
  "Contains logic for routes."
  (:require [urdar.views :as views]
            [cemerick.friend :as friend]))

(defn index [req]
  (views/index req))

(defn login
  "Redirects to index page in case user is already logged in."
  [request]
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    {:status 302 :headers {"Location" "/"}}
    views/login))

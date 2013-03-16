(ns urdar.controllers.site
  "Contains logic for routes."
  (:require [urdar.views :as views]
            [urdar.helpers.external-api :as api]
            [urdar.helpers.utils :as u]
            [cemerick.friend :as friend]))

(defmulti get-user-mail
  "Retrieves e-mail address of user using external API (OAuth provider)."
  u/get-roles)

(defmethod get-user-mail #{:urdar/google-user} [request]
  (api/google-get-mail request))

(defmethod get-user-mail #{:urdar/github-user} [request]
  (api/github-get-mail request))

(defmethod get-user-mail :default [_]
  "Unknown user.")

(defn index [request]
  (views/index (get-user-mail request)))

(defn login
  "Redirects to index page in case user is already logged in."
  [request]
  (if (friend/authorized? #{:urdar/user} (friend/identity request))
    {:status 302 :headers {"Location" "/"}}
    views/login))

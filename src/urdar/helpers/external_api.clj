(ns urdar.helpers.external-api
  "Different API calls to external services."
  (:require [urdar.helpers.utils :as u]
            [clj-http.client :as http]
            [cheshire.core :as json]))

;; ## Helpers

(defn api-get
  "Requests a given operation using given API. It is assumed that operation
begins with slash."
  ([^String api-root ^String op ^String access-token]
     (api-get api-root op {} access-token))
  ([^String api-root ^String op req-options ^String access-token]
     (assert (.startsWith op "/"))
     (some-> (str api-root "%s%saccess_token=%s")
             (format op (if-not (.contains op "?") "?" "&") access-token)
             (http/get req-options)
             :body
             (json/parse-string true))))

;; ## Github API

(defn github-get-mail
  "Retrieves a mail of currently logged-in user using Github API."
  [request]
  (some->> (u/get-access-token request)
           (api-get "https://api.github.com" "/user/emails"
                    {:accept :application/vnd.github.v3})
           (filter :primary)
           first
           :email))

;; ## Google API

(defn google-get-mail
  "Retrieves a mail of currently logged-in user using Github API."
  [request]
  (some->> (u/get-access-token request)
           (api-get "https://www.googleapis.com/oauth2/v1"
                    "/userinfo?alt=json")
           :email))

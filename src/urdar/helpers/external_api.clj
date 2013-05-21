(ns urdar.helpers.external-api
  "Different API calls to external services."
  (:require [urdar.helpers.utils :as u]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [net.cgrand.enlive-html :as html]))

;;; ## Generic API caller

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

;;; ## Specific API calls

(defmulti get-user-mail-address
  "Retrieves e-mail address of user using external API (OAuth provider)."
  u/get-roles)

(defmethod get-user-mail-address #{:urdar/google-user} [request]
  (some->> (u/get-access-token request)
           (api-get "https://www.googleapis.com/oauth2/v1"
                    "/userinfo?alt=json")
           :email))

(defmethod get-user-mail-address #{:urdar/github-user} [request]
  (some->> (u/get-access-token request)
           (api-get "https://api.github.com" "/user/emails"
                    {:accept :application/vnd.github.v3})
           (filter :primary)
           first
           :email))

(defmethod get-user-mail-address :default [_]
  "Unknown user.")

(defn retrieve-title
  "Retrieves a title of the given page."
  [url]
  (some-> (try
            (html/html-resource (java.net.URL. url))
            (catch Exception _ nil))
          (html/select [:head :title])
          first
          html/text))

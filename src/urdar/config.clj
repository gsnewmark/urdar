(ns urdar.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [friend-oauth2.workflow :refer [format-config-uri]]))

(defn read-config
  "Reads edn config file."
  []
  (edn/read (java.io.PushbackReader. (io/reader (io/resource "config.edn")))))

(def config
  "Application's configuration."
  (read-config))

(def github-uri-config
  {:authentication-uri
   {:url (get-in config [:github-urls :authentication-url])
    :query {:client_id (get-in config [:github-client-config :client-id])
            :response_type "code"
            :redirect_uri (format-config-uri (:github-client-config config))
            :scope "user:email"}}
   :access-token-uri
   {:url (get-in config [:github-urls :access-token-url])
    :query {:client_id (get-in config [:github-client-config :client-id])
            :client_secret (get-in config [:github-client-config :client-secret])
            :grant_type "authorization_code"
            :redirect_uri (format-config-uri (:github-client-config config))
            :code ""}}})

(def google-uri-config
  {:authentication-uri
   {:url (get-in config [:google-urls :authentication-url])
    :query {:client_id (get-in config [:google-client-config :client-id])
            :response_type "code"
            :redirect_uri (format-config-uri (:google-client-config config))
            :scope "https://www.googleapis.com/auth/userinfo.email"}}
   :access-token-uri
   {:url (get-in config [:google-urls :access-token-url])
    :query {:client_id (get-in config [:google-client-config :client-id])
            :client_secret (get-in config [:google-client-config :client-secret])
            :grant_type "authorization_code"
            :redirect_uri (format-config-uri (:google-client-config config))
            :code ""}}})

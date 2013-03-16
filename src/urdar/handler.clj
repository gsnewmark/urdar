(ns urdar.handler
  (:require [urdar.routes.site :as site]
            [compojure.core :as compojure :refer [defroutes ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend :as friend]
            [friend-oauth2.workflow :as oauth2]))

(def config-auth {:roles #{::user}})

(def github-client-config
  {:client-id ""
   :client-secret ""
   :callback {:domain "http://localhost:3000" :path "/oauth2callback"}})

(def github-uri-config
  {:authentication-uri
   {:url "https://github.com/login/oauth/authorize"
    :query {:client_id (:client-id github-client-config)
            :response_type "code"
            :redirect_uri (oauth2/format-config-uri github-client-config)
            :scope "user:email"}}

   :access-token-uri
   {:url "https://github.com/login/oauth/access_token"
    :query {:client_id (:client-id github-client-config)
            :client_secret (:client-secret github-client-config)
            :grant_type "authorization_code"
            :redirect_uri (oauth2/format-config-uri github-client-config)
            :code ""}}})

(def google-client-config
  {:client-id ""
   :client-secret ""
   :callback {:domain "http://localhost:3000" :path "/oauth2callback"}})

(def google-uri-config
  {:authentication-uri
   {:url "https://accounts.google.com/o/oauth2/auth"
    :query {:client_id (:client-id google-client-config)
            :response_type "code"
            :redirect_uri (oauth2/format-config-uri google-client-config)
            :scope "https://www.googleapis.com/auth/userinfo.email"}}

   :access-token-uri
   {:url "https://accounts.google.com/o/oauth2/token"
    :query {:client_id (:client-id google-client-config)
            :client_secret (:client-secret google-client-config)
            :grant_type "authorization_code"
            :redirect_uri (oauth2/format-config-uri google-client-config)
            :code ""}}})

(defroutes app
  (compojure/context "/" request
                     (friend/wrap-authorize site/registered #{::user}))
  (route/resources "/")
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "<h1>Page not found</h1>"))

(def app-with-login (compojure/routes site/guest app))

(def secured-app
  (-> app-with-login
      (friend/authenticate
       {:workflows
        [(oauth2/workflow
          {:login-uri "/login-github"
           :client-config github-client-config
           :uri-config github-uri-config
           :access-token-parsefn #(-> % :body
                                      ring.util.codec/form-decode
                                      (get "access_token"))
           :config-auth config-auth})

         (oauth2/workflow
          {:login-uri "/login-google"
           :client-config google-client-config
           :uri-config google-uri-config
           :config-auth config-auth})]})
      handler/site))

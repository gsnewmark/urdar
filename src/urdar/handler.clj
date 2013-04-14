(ns urdar.handler
  (:require [urdar.config :as cfg]
            [urdar.routes :as routes]
            [compojure.core :as compojure :refer [defroutes ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend :as friend]
            [friend-oauth2.workflow :as oauth2]
            [ring.middleware.edn :as edn]))

(derive :urdar/github-user :urdar/user)
(derive :urdar/google-user :urdar/user)

(defroutes registered-routes
  ;; TODO wrap friend
  (compojure/context "/_" request
                     routes/internal-api)
  (compojure/context "/" request
                     (friend/wrap-authorize routes/registered #{:urdar/user}))
  (route/resources "/")
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "<h1>Page not found</h1>"))

(def routes (compojure/routes routes/guest registered-routes))

(defn wrap-request-log [handler]
  (fn [request]
    (println (str (java.util.Date.) ": " request))
    (handler request)))

(def app
  (-> routes
      wrap-request-log
      (friend/authenticate
       {:workflows
        [(oauth2/workflow
          {:login-uri "/login-github"
           :client-config (:github-client-config cfg/config)
           :uri-config cfg/github-uri-config
           :access-token-parsefn #(-> % :body
                                      ring.util.codec/form-decode
                                      (get "access_token"))
           :config-auth {:roles #{:urdar/github-user}}})
         (oauth2/workflow
          {:login-uri "/login-google"
           :client-config (:google-client-config cfg/config)
           :uri-config cfg/google-uri-config
           :config-auth {:roles #{:urdar/google-user}}})]})
      ;; TODO session config
      handler/site
      edn/wrap-edn-params))

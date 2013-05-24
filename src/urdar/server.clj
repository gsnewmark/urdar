(ns urdar.server
  (:require [urdar.config :as cfg]
            [urdar.routes :as routes]
            [urdar.search :as s]
            [compojure.core :as compojure :refer [defroutes ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend :as friend]
            [friend-oauth2.workflow :as oauth2]
            [ring.middleware.edn :as edn]
            [ring.middleware.file-info :as fi]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(derive :urdar/github-user :urdar/user)
(derive :urdar/google-user :urdar/user)

(defroutes routes
  (compojure/context "/_" request
                     (friend/wrap-authorize routes/internal-api #{:urdar/user}))
  (compojure/context "/" request routes/site)
  (route/resources "/")
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-request-log [handler]
  (fn [request]
    (println (str "Request on " (java.util.Date.) ": " request))
    (handler request)))

(defn wrap-response-log [handler]
  (fn [request]
    (let [r (handler request)]
      (println (str "Response on " (java.util.Date.) ": " r))
      r)))

;;; TODO middleware to catch exceptions and format them as edn response

(def app
  (-> routes
      wrap-response-log
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
           :config-auth {:roles #{:urdar/google-user}}})]
        :login-uri "/"})
      ;; TODO session config
      handler/site
      edn/wrap-edn-params
      (fi/wrap-file-info)))

(defn -main
  [& [port]]
  (let [port (Integer. (or port
                           (System/getenv "PORT")
                           5000))]
    (s/init-connection)
    (jetty/run-jetty #'app {:port  port
                           :join? false})))

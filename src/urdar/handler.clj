(ns urdar.handler
  (:require [urdar.routes.site :as site]
            [compojure.core :as compojure :refer [defroutes ANY]]
            [compojure.route :as route]
            [cemerick.friend :as friend]))

(derive ::admin ::user)

(defroutes app
  (compojure/context "/user" request
    (friend/wrap-authorize site/registered #{::user}))
  (route/resources "/")
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))
  (route/not-found "<h1>Page not found</h1>"))

(def secured-app
  (-> (compojure/routes site/guest app)
      (friend/authenticate
       {:workflows []})
      compojure.handler/site))

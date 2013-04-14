(ns urdar.routes
  (:require [urdar.controllers.site :as site]
            [urdar.controllers.api :as api]
            [compojure.core :refer [defroutes GET POST]]))

(defroutes registered
  (GET "/" req site/index))

(defroutes guest
  (GET "/login" req site/login))

(defroutes internal-api
  (GET "/bookmarks" {{e-mail :e-mail} :session} (api/get-bookmarks e-mail))
  (POST "/add-bookmark" [link & {{e-mail :e-mail} :session}] (api/add-bookmark e-mail link)))

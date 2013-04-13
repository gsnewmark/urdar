(ns urdar.routes
  (:require [urdar.controllers.site :as site]
            [urdar.controllers.api :as api]
            [compojure.core :refer [defroutes GET POST]]))

(defroutes registered
  (GET "/" req site/index))

(defroutes guest
  (GET "/login" req site/login))

(defroutes internal-api
  (GET "/bookmarks" req api/get-bookmarks)
  (POST "/add-bookmark" req api/add-bookmark))

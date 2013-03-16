(ns urdar.routes.site
  "Routes for the basic functionality of app available to users through
web interface."
  (:require [urdar.controllers.site :as controllers]
            [compojure.core :refer [defroutes GET]]))

(defroutes registered
  (GET "/" req controllers/index))

(defroutes guest
  (GET "/login" req controllers/login))

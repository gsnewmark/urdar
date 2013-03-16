(ns urdar.routes.site
  "Routes for the basic functionality of app available to users through
web interface."
  (:require [urdar.views :as views]
            [compojure.core :refer [defroutes GET]]))

(defroutes registered
  (GET "/" [] views/index))

(defroutes guest
  (GET "/login" [] views/login))

(ns urdar.routes.site
  "Routes for the basic functionality of app available to users through
web interface."
  (:require [urdar.views :as views]
            [cemerick.friend :as friend]
            [compojure.core :refer [defroutes GET]]))

(defroutes registered
  (GET "/" req (str (friend/identity req))))

(defroutes guest
  (GET "/login" req views/login))

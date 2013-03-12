(ns urdar.routes
  (:require [urdar.views :as views]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]))

(defroutes app
  (GET "/" [] views/index)
  (route/not-found "<h1>Page not found</h1>"))

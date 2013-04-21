(ns urdar.routes
  (:require [urdar.controllers.site :as site]
            [urdar.controllers.api :as api]
            [compojure.core :refer [defroutes GET POST]]))

(defroutes site
  (GET "/" req site/index))

(defroutes internal-api
  (GET "/bookmarks/:skip-bookmarks/:bookmarks-to-fetch"
       [bookmarks-to-fetch skip-bookmarks
        :as {{e-mail :e-mail} :session}]
       (api/get-bookmarks e-mail skip-bookmarks bookmarks-to-fetch))
  (POST "/add-bookmark" [link :as {{e-mail :e-mail} :session}]
        (api/add-bookmark! e-mail link)))

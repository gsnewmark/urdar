(ns urdar.routes
  (:require [urdar.controllers.site :as site]
            [urdar.controllers.api :as api]
            [compojure.core :refer [defroutes GET POST DELETE]]))

(defroutes site
  (GET "/" req site/index))

(defroutes internal-api
  (GET "/bookmarks/:skip-bookmarks/:bookmarks-to-fetch/"
       [bookmarks-to-fetch skip-bookmarks
        :as {{e-mail :e-mail} :session}]
       (api/get-bookmarks e-mail skip-bookmarks bookmarks-to-fetch))
  (GET "/bookmarks/:skip-bookmarks/:bookmarks-to-fetch/:tag"
       [bookmarks-to-fetch skip-bookmarks tag
        :as {{e-mail :e-mail} :session}]
       (api/get-tagged-bookmarks e-mail tag skip-bookmarks bookmarks-to-fetch))
  (POST "/add-bookmark" [link :as {{e-mail :e-mail} :session}]
        (api/add-bookmark! e-mail link))
  (DELETE "/delete-bookmark" [link :as {{e-mail :e-mail} :session}]
          (api/delete-bookmark! e-mail link))
  (POST "/add-tag" [link tag :as {{e-mail :e-mail} :session}]
        (api/add-tag! e-mail tag link))
  (DELETE "/remove-tag" [link tag :as {{e-mail :e-mail} :session}]
        (api/remove-tag! e-mail tag link)))

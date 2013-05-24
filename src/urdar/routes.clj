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
  ;; TODO tag, query should be "body" parameters (not part of path)
  (GET "/bookmarks/:skip-bookmarks/:bookmarks-to-fetch/:tag"
       [bookmarks-to-fetch skip-bookmarks tag
        :as {{e-mail :e-mail} :session}]
       (api/get-tagged-bookmarks e-mail tag skip-bookmarks bookmarks-to-fetch))
  (GET "/bookmarks/:skip-bookmarks/:bookmarks-to-fetch/:tag/:query"
       [bookmarks-to-fetch skip-bookmarks query
        :as {{e-mail :e-mail} :session}]
       (api/search-bookmarks e-mail query skip-bookmarks bookmarks-to-fetch))
  (GET "/tags" {{e-mail :e-mail} :session} (api/get-tags e-mail))
  (GET "/recommendations" {{e-mail :e-mail} :session}
       (api/get-recommendations e-mail))
  (POST "/bookmarks" [link :as {{e-mail :e-mail} :session}]
        (api/add-bookmark! e-mail link))
  (POST "/tags" [link tag :as {{e-mail :e-mail} :session}]
        (api/add-tag! e-mail tag link))
  (POST "/notes" [link note :as {{e-mail :e-mail} :session}]
        (api/add-note! e-mail link note))
  (DELETE "/bookmarks" [link :as {{e-mail :e-mail} :session}]
          (api/delete-bookmark! e-mail link))
  (DELETE "/tags" [link tag :as {{e-mail :e-mail} :session}]
          (api/remove-tag! e-mail tag link)))

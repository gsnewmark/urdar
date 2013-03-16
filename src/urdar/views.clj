(ns urdar.views
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate index "templates/index.html" [user]
  [:span#user] (html/content user))

(html/deftemplate login "templates/login.html" [_])

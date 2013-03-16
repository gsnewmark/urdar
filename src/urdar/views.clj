(ns urdar.views
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate index "templates/index.html" [_])
(html/deftemplate login "templates/login.html" [_])

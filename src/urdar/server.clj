(ns urdar.server
  (:require [urdar.handler :as h]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(defn -main
  [& [port]]
  (let [port (Integer. (or port
                           (System/getenv "PORT")
                           5000))]
    (jetty/run-jetty #'h/app {:port  port
                              :join? false})))

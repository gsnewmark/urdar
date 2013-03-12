(ns urdar.server
  (:require [urdar.routes :as r]
            [ring.adapter.jetty :as jetty])
  (:gen-class))

(defn -main
  [& [port]]
  (let [port (Integer. (or port
                           (System/getenv "PORT")
                           5000))]
    (jetty/run-jetty #'r/app {:port  port
                              :join? false})))

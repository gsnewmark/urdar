(defproject urdar "0.1.0-SNAPSHOT"
  :description "Web bookmarks organizer."
  :url "https://github.com/gsnewmark/urdar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.2.0-beta1"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [com.cemerick/friend "0.1.5"]
                 [friend-oauth2 "0.0.3"]
                 [clj-http "0.7.0"]
                 [cheshire "5.0.2"]
                 [clojurewerkz/neocons "1.1.0"]
                 [fogus/ring-edn "0.2.0-SNAPSHOT"]
                 [enfocus "2.0.0-SNAPSHOT"]
                 [shoreleave "0.3.0"]]
  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "0.3.0"]
            [lein-marginalia "0.7.1"]]
  :main urdar.server
  :ring {:handler urdar.handler/app}
  :resource-paths ["resources"]
  :cljsbuild
  {:builds
   [{:source-paths ["src/urdar/client"]
     :compiler {:output-to "resources/public/js/main.js"
                :optimizations :whitespace
                :pretty-print true}}]})

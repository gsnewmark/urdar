(defproject urdar "0.1.0-SNAPSHOT"
  :description "Web bookmarks organizer."
  :url "https://github.com/gsnewmark/urdar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.1.8"]
                 [compojure "1.1.5" :exclusions [org.clojure/clojure
                                                 ring/ring-core]]
                 [enlive "1.1.1" :exclusions [org.clojure/clojure]]
                 [com.cemerick/friend "0.1.4" :exclusions [org.clojure/clojure
                                                           ring/ring-core]]
                 [friend-oauth2 "0.0.3" :exclusions [org.clojure/clojure
                                                     com.cemerick/friend
                                                     ring]]]
  :plugins [[lein-ring "0.8.3"]
            [lein-marginalia "0.7.1"]]
  :main urdar.server
  :ring {:handler urdar.handler/secured-app}
  :resource-paths ["resources"])

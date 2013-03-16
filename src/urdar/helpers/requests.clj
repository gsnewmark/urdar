(ns urdar.helpers.requests
  "Different API calls to external services."
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn github-get [^String access-token ^String op]
  "Requests a given operation using Github API."
  (some-> "https://api.github.com/%s%saccess_token=%s"
          (format op (if-not (.contains op "?") "?" "&") access-token)
          http/get
          :body
          (json/parse-string true)))

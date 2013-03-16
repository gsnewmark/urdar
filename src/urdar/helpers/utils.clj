(ns urdar.helpers.utils
  "Different handy utils."
  (:require [cemerick.friend :as friend]))

(defn get-access-token
  "Retrieves an access token from given Ring request."
  [request]
  (when-let [identity (friend/identity request)]
    (let [{:keys [current authentications]} identity]
      (get-in authentications [current :access_token]))))

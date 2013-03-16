(ns urdar.helpers.utils
  "Different handy utils."
  (:require [cemerick.friend :as friend]))

(defn get-identity-part
  "Retrieves specific part of user's identity (created by friend library)."
  [part request]
  (when-let [identity (friend/identity request)]
    (let [{:keys [current authentications]} identity]
      (get-in authentications [current part]))))

(def get-access-token
  "Retrieves an access token from given Ring request."
  (partial get-identity-part :access_token))

(def get-roles
  "Retrieves user's roles from given Ring request."
  (partial get-identity-part :roles))

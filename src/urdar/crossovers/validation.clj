(ns urdar.crossovers.validation
  "Functions to validate input.")


(defn valid-url?
  "Checks whether the given string is a correct URL."
  [s]
  ;; Regex from http://mathiasbynens.be/demo/url-regex by @imme_emosol
  (re-matches #"(https?|ftp)://(-\.)?([^\s/?\.#]+\.?)+(/[^\s]*)?$" s))

(defn valid-tag?
  "Checks whether the given string is a correct tag."
  [s]
  (re-matches #"^[\w-]{1,50}$" s))

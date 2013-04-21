(ns urdar.crossovers.validation
  "Functions to validate input.")


(defn valid-url?
  "Validates that given string is a correct URL."
  [s]
  ;; Regex from http://mathiasbynens.be/demo/url-regex by @imme_emosol
  (seq? (re-seq #"(https?|ftp)://(-\.)?([^\s/?\.#-]+\.?)+(/[^\s]*)?$" s)))

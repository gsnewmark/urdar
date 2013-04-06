(ns urdar.datastore-test
  (:require [urdar.datastore :as ds])
  (:use [clojure.test]))

(use-fixtures :once (fn [f] (ds/init ds/datastore) (f)))

(deftest user-creation
  (let [e-mail "bob@example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is (ds/user? ds/datastore e-mail))))

(deftest user-bookmark-creation
  (let [e-mail "alice@example.com"
        link "http://page.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:data :link])
        link))
    (is (ds/link-exists? ds/datastore link))
    (is (contains? (into #{} (ds/get-bookmarks ds/datastore e-mail)) link))))

(deftest user-tag-creation
  (let [e-mail "charlie@example.com"
        tag "my-tag"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is (= (get-in (ds/create-tag ds/datastore e-mail tag) [:data :name])))
    (is (ds/tag-exists? ds/datastore e-mail tag))
    (is (contains? (into #{} (ds/get-tags ds/datastore e-mail)) tag))))

(deftest user-tag-bookmark-creation
  (let [e-mail "dave@example.com"
        tag "my-tag"
        link "http://page2.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is (= (get-in (ds/create-tag ds/datastore e-mail tag) [:data :name])))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:data :link])
        link))
    (ds/tag-bookmark ds/datastore e-mail tag link)
    (is (contains?
         (into #{} (ds/get-tagged-bookmarks ds/datastore e-mail tag))
         link))))

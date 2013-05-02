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
        link "http://pageA.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:link])
        link))
    (is (ds/link-exists? ds/datastore link))
    (is (contains?
         (into #{} (map :link (ds/get-bookmarks ds/datastore e-mail)))
         link))))

(deftest user-bookmark-existence
  (let [e-mail "alice@example.com"
        link1 "http://pageA1.example.com"
        link2 "http://pageA2.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link1) [:link])
        link1))
    (is (= (ds/bookmark-exists? ds/datastore e-mail link1) true))
    (is (= (ds/bookmark-exists? ds/datastore e-mail link2) false))))

(deftest user-bookmark-deletion
  (let [e-mail "alice@example.com"
        link "http://page-to-remove.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:link])
        link))
    (is (ds/link-exists? ds/datastore link))
    (is (contains?
         (into #{} (map :link (ds/get-bookmarks ds/datastore e-mail)))
         link))
    (ds/delete-bookmark ds/datastore e-mail link)
    (is (not (contains?
              (into #{} (map :link (ds/get-bookmarks ds/datastore e-mail)))
              link)))))

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
        link "http://pageD.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is (= (get-in (ds/create-tag ds/datastore e-mail tag) [:data :name])))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:link])
        link))
    (ds/tag-bookmark ds/datastore e-mail tag link)
    (is (ds/bookmark-tagged? ds/datastore e-mail tag link))))

(deftest user-tag-bookmark-deletion
  (let [e-mail "pit@example.com"
        tag "my-tag2"
        link "http://pageE.example.com"]
    (is (= (get-in (ds/create-user ds/datastore e-mail) [:data :e-mail])
           e-mail))
    (is (= (get-in (ds/create-tag ds/datastore e-mail tag) [:data :name])))
    (is
     (= (get-in (ds/create-bookmark ds/datastore e-mail link) [:link])
        link))
    (ds/tag-bookmark ds/datastore e-mail tag link)
    (is (ds/bookmark-tagged? ds/datastore e-mail tag link))
    (ds/untag-bookmark ds/datastore e-mail tag link)
    (is (not (ds/bookmark-tagged? ds/datastore e-mail tag link)))))

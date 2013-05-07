(ns urdar.db-test
  (:require [urdar.db :as db]
            [clojure.test :refer :all]))

(declare clean-db)

(use-fixtures :each (fn [f] (clean-db) (f)))

(deftest unregistered-user-check
  (let [e-mail "bob@example.com"]
    (is (not (db/user-registered? db/u e-mail)))))

(deftest user-registration
  (let [e-mail "bob@example.com"
        r (db/register-user db/u e-mail)]
    (is (db/user-registered? db/u e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

(deftest user-unregistration
  (let [e-mail "bob@example.com"]
    (db/register-user db/u e-mail)
    (db/unregister-user db/u e-mail)
    (is (not (db/user-registered? db/u e-mail)))))

(deftest user-retrieval
  (let [e-mail "bob@example.com"
        _ (db/register-user db/u e-mail)
        r (db/get-user db/u e-mail)]
    (is (db/user-registered? db/u e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

(deftest unadded-bookmark-check
  (let [e-mail "bob@example.com"
        link "http://example.com"]
    (is (not (db/bookmark-exists? db/u e-mail link)))))

(deftest bookmark-addition
  (let [e-mail "bob@example.com"
        link "http://example.com"
        _ (db/register-user db/u e-mail)
        r (db/add-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest bookmark-removal
  (let [e-mail "bob@example.com"
        link "http://example.com"]
    (db/register-user db/u e-mail)
    (db/add-bookmark db/u e-mail link)
    (db/delete-bookmark db/u e-mail link)
    (is (not (db/bookmark-exists? db/u e-mail link)))))

(deftest bookmark-retrieval
  (let [e-mail "bob@example.com"
        link "http://example.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest all-bookmarks-retrieval
  (let [e-mail "bob@example.com"
        link1 "http://page1.example.com"
        link2 "http://page2.example.com"
        link3 "http://page3.example.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        r (db/get-bookmarks db/u e-mail)]
    (is (every? #(= urdar.db.Bookmark (class %)) e))
    (is (= #{link1 link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? db/u e-mail (:link %)) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest some-bookmarks-retrieval
  (let [e-mail "bob@example.com"
        link1 "http://page1.example.com"
        link2 "http://page2.example.com"
        link3 "http://page3.example.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        r (db/get-bookmarks db/u e-mail 1 2)]
    (is (every? #(= urdar.db.Bookmark (class %)) e))
    (is (= #{link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? db/u e-mail (:link %)) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest bookmark-update
  (let [e-mail "bob@example.com"
        link "http://example.com"
        title "Example"
        desc "Long description."
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/update-bookmark db/u e-mail link :title title :description desc)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (= title (:title r)))
    (is (= desc (:description r)))))

(deftest bookmark-tagging
  (let [e-mail "bob@example.com"
        link "http://example.com"
        tag "tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        r (db/tag-bookmark db/u e-mail link tag)]
    (is (db/tag-exists? db/u e-mail tag))
    (is (db/bookmark-tagged? db/u e-mail link tag))
    (is (= urdar.db.Bookmark (class r)))
    (is (contains? (:tags r) tag))))

(deftest bookmark-untagging
  (deftest bookmark-tagging
  (let [e-mail "bob@example.com"
        link "http://example.com"
        tag "tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag)
        _ (db/untag-bookmark db/u e-mail link tag)
        r (db/get-bookmark db/u e-mail link)]
    (is (not (db/bookmark-tagged? db/u e-mail link tag)))
    (is (not (db/tag-exists? db/u e-mail tag)))
    (is (not (contains? (:tags r) tag))))))

(deftest all-tagged-bookmarks-retrieval
  (let [e-mail "bob@example.com"
        link1 "http://page1.example.com"
        link2 "http://page2.example.com"
        link3 "http://page3.example.com"
        tag "tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        _ (db/tag-bookmark db/u e-mail link1 tag)
        _ (db/tag-bookmark db/u e-mail link2 tag)
        _ (db/tag-bookmark db/u e-mail link3 tag)
        r (db/get-tagged-bookmarks db/u e-mail tag)]
    (is (every? #(= urdar.db.Bookmark (class %)) e))
    (is (= #{link1 link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-tagged? db/u e-mail (:link %) tag) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(contains? (:tags %) tag) r))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest some-tagged-bookmarks-retrieval
  (let [e-mail "bob@example.com"
        link1 "http://page1.example.com"
        link2 "http://page2.example.com"
        link3 "http://page3.example.com"
        tag "tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        _ (db/tag-bookmark db/u e-mail link1 tag)
        _ (db/tag-bookmark db/u e-mail link2 tag)
        _ (db/tag-bookmark db/u e-mail link3 tag)
        r (db/get-tagged-bookmarks db/u e-mail tag 1 2)]
    (is (every? #(= urdar.db.Bookmark (class %)) e))
    (is (= #{link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-tagged? db/u e-mail (:link %) tag) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(contains? (:tags %) tag) r))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest tags-retrieval
  (let [e-mail "bob@example.com"
        link1 "http://example.com"
        tag1 "tag1"
        tag2 "tag2"
        tag3 "tag3"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag1)
        _ (db/tag-bookmark db/u e-mail link tag2)
        _ (db/tag-bookmark db/u e-mail link tag3)
        r (db/get-tags self e-mail)]
    (is (= #{tag1 tag2 tag3} r))))

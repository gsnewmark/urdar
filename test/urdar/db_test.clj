(ns urdar.db-test
  (:require [urdar.db :as db]
            [clojure.test :refer :all]))

(deftest unregistered-user-check
  (let [e-mail "bob@example.com"]
    (is (not (db/user-registered? db/u e-mail)))))

(deftest user-registration
  (let [e-mail "charlie@example.com"
        r (db/register-user db/u e-mail)]
    (is (db/user-registered? db/u e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

;;; TODO check that all tags of deleted user are also deleted

(deftest user-unregistration
  (let [e-mail "alice@example.com"]
    (db/register-user db/u e-mail)
    (db/unregister-user db/u e-mail)
    (is (not (db/user-registered? db/u e-mail)))))

(deftest user-retrieval
  (let [e-mail "jersey@example.com"
        _ (db/register-user db/u e-mail)
        r (db/get-user db/u e-mail)]
    (is (db/user-registered? db/u e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

(deftest unadded-bookmark-check
  (let [e-mail "tim@example.com"
        link "http://example1.com"]
    (is (not (db/bookmark-exists? db/u e-mail link)))))

(deftest bookmark-addition
  (let [e-mail "alex@example.com"
        link "http://example2.com"
        _ (db/register-user db/u e-mail)
        r (db/add-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest bookmark-removal
  (let [e-mail "dirk@example.com"
        link "http://example3.com"]
    (db/register-user db/u e-mail)
    (db/add-bookmark db/u e-mail link)
    (db/delete-bookmark db/u e-mail link)
    (is (not (db/bookmark-exists? db/u e-mail link)))))

(deftest bookmark-retrieval
  (let [e-mail "bobby@example.com"
        link "http://example4.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest all-bookmarks-retrieval
  (let [e-mail "mike@example.com"
        link1 "http://mike.page1.example.com"
        link2 "http://mike.page2.example.com"
        link3 "http://mike.page3.example.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        r (db/get-bookmarks db/u e-mail)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? db/u e-mail (:link %)) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest some-bookmarks-retrieval
  (let [e-mail "timmy@example.com"
        link1 "http://timmy.page1.example.com"
        link2 "http://timmy.page2.example.com"
        link3 "http://timmy.page3.example.com"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark db/u e-mail link2)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark db/u e-mail link3)
        r (db/get-bookmarks db/u e-mail 1 2)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? db/u e-mail (:link %)) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest user-unregistration-with-bookmarks
  (let [e-mail "kyle@example.com"
        link1 "http://kyle.page1.example.com"
        link2 "http://kyle.page2.example.com"]
    (db/register-user db/u e-mail)
    (db/add-bookmark db/u e-mail link1)
    (db/add-bookmark db/u e-mail link2)
    (db/unregister-user db/u e-mail)
    (is (not (db/user-registered? db/u e-mail)))
    (is (not (db/bookmark-exists? db/u e-mail link1)))
    (is (not (db/bookmark-exists? db/u e-mail link2)))))

(deftest bookmark-update-title-description
  (let [e-mail "emilie@example.com"
        link "http://emilie.example.com"
        title "Example"
        desc "Long description."
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/update-bookmark db/u e-mail link title desc)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (= title (:title r)))
    (is (= desc (:note r)))))

(deftest bookmark-update-title
  (let [e-mail "johny@example.com"
        link "http://johny.example.com"
        title "Example"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/update-bookmark db/u e-mail link title nil)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (= title (:title r)))
    (is (nil? (:note r)))))

(deftest bookmark-update-description
  (let [e-mail "connor@example.com"
        link "http://connor.example.com"
        desc "Long description."
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/update-bookmark db/u e-mail link nil desc)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (nil? (:title r)))
    (is (= desc (:note r)))))

(deftest bookmark-tagging
  (let [e-mail "buffy@example.com"
        link "http://buffy.example.com"
        tag "buffy.tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-tagged? db/u e-mail link tag))
    (is (= urdar.db.Bookmark (class r)))
    (is (= #{tag} (into #{} (:tags r))))))

(deftest bookmark-untagging
  (let [e-mail "ramires@example.com"
        link "http://ramires.example.com"
        tag "ramires.tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag)
        _ (db/untag-bookmark db/u e-mail link tag)
        r (db/get-bookmark db/u e-mail link)]
    (is (not (db/bookmark-tagged? db/u e-mail link tag)))
    (is (not ((into #{} (:tags r)) tag)))))

(deftest bookmark-with-tags-retrieval
  (let [e-mail "grace@example.com"
        link "http://grace.example4.com"
        tag1 "grace.tag1"
        tag2 "grace.tag2"
        tag3 "grace.tag3"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag1)
        _ (db/tag-bookmark db/u e-mail link tag2)
        _ (db/tag-bookmark db/u e-mail link tag3)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (= #{tag1 tag2 tag3} (into #{} (:tags r))))
    (is (not (nil? (:date-added r))))))

(deftest tags-retrieval
  (let [e-mail "kirk@example.com"
        link "http://kirk.example.com"
        tag1 "kirk.tag1"
        tag2 "kirk.tag2"
        tag3 "kirk.tag3"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        _ (db/tag-bookmark db/u e-mail link tag1)
        _ (db/tag-bookmark db/u e-mail link tag2)
        _ (db/tag-bookmark db/u e-mail link tag3)
        r (db/get-tags db/u e-mail)]
    (is (= #{tag1 tag2 tag3} (into #{} r)))))

(deftest bookmarks-with-tags-removal
  (let [e-mail "john@example.com"
        link1 "http://john1.example.com"
        link2 "http://john2.example.com"
        link3 "http://john3.example.com"
        tag1 "john.tag1"
        tag2 "john.tag2"
        tag3 "john.tag3"]
     (db/register-user db/u e-mail)
     (db/add-bookmark db/u e-mail link1)
     (db/add-bookmark db/u e-mail link2)
     (db/add-bookmark db/u e-mail link3)
     (db/tag-bookmark db/u e-mail link1 tag1)
     (db/tag-bookmark db/u e-mail link1 tag2)
     (db/tag-bookmark db/u e-mail link2 tag2)
     (db/tag-bookmark db/u e-mail link3 tag2)
     (db/tag-bookmark db/u e-mail link3 tag3)
     (is (= #{tag1 tag2 tag3} (into #{} (db/get-tags db/u e-mail))))
     (db/delete-bookmark db/u e-mail link3)
     (is (= #{tag1 tag2} (into #{} (db/get-tags db/u e-mail))))
     (db/delete-bookmark db/u e-mail link1)
     (is (= #{tag2} (into #{} (db/get-tags db/u e-mail))))
     (db/delete-bookmark db/u e-mail link2)
     (is (= #{} (into #{} (db/get-tags db/u e-mail))))))

(deftest all-tagged-bookmarks-retrieval
  (let [e-mail "connel@example.com"
        link1 "http://connel.page1.example.com"
        link2 "http://connel.page2.example.com"
        link3 "http://connel.page3.example.com"
        tag "connel.tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (db/add-bookmark db/u e-mail link2)
        _ (db/add-bookmark db/u e-mail link3)
        _ (db/tag-bookmark db/u e-mail link1 tag)
        _ (db/tag-bookmark db/u e-mail link2 tag)
        _ (db/tag-bookmark db/u e-mail link3 tag)
        r (db/get-tagged-bookmarks db/u e-mail tag)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-tagged? db/u e-mail (:link %) tag) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #((into #{} (:tags %)) tag) r))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest some-tagged-bookmarks-retrieval
  (let [e-mail "cyrus@example.com"
        link1 "http://cyrus.page1.example.com"
        link2 "http://cyrus.page2.example.com"
        link3 "http://cyrus.page3.example.com"
        tag "cyrus.tag"
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link1)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark db/u e-mail link2)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark db/u e-mail link3)
        _ (db/tag-bookmark db/u e-mail link1 tag)
        _ (db/tag-bookmark db/u e-mail link2 tag)
        _ (db/tag-bookmark db/u e-mail link3 tag)
        r (db/get-tagged-bookmarks db/u e-mail tag 1 2)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2} (into #{} (map :link r))))
    (is (every? #(db/bookmark-tagged? db/u e-mail (:link %) tag) r))
    (is (= #{e-mail} (into #{} (map :e-mail r))))
    (is (every? #((into #{} (:tags %)) tag) r))
    (is (every? #(not (nil? (:date-added %))) r))))

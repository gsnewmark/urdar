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

;;; TODO check tags removal

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
        _ (Thread/sleep 500)
        _ (db/add-bookmark db/u e-mail link2)
        _ (Thread/sleep 500)
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

;;; TODO tagged bookmarks retrieval

(deftest bookmark-update-title-description
  (let [e-mail "emilie@example.com"
        link "http://emilie.example.com"
        title "Example"
        desc "Long description."
        _ (db/register-user db/u e-mail)
        _ (db/add-bookmark db/u e-mail link)
        - (Thread/sleep 500)
        _ (db/update-bookmark db/u e-mail link title desc)
        _ (Thread/sleep 500)
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
        - (Thread/sleep 500)
        _ (db/update-bookmark db/u e-mail link title nil)
        _ (Thread/sleep 500)
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
        - (Thread/sleep 500)
        _ (db/update-bookmark db/u e-mail link nil desc)
        _ (Thread/sleep 500)
        r (db/get-bookmark db/u e-mail link)]
    (is (db/bookmark-exists? db/u e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= e-mail (:e-mail r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (nil? (:title r)))
    (is (= desc (:note r)))))

(comment (deftest bookmark-tagging
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
             (is (every? #(= urdar.db.Bookmark (class %)) r))
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
             (is (every? #(= urdar.db.Bookmark (class %)) r))
             (is (= #{link1 link2} (into #{} (map :link r))))
             (is (every? #(db/bookmark-tagged? db/u e-mail (:link %) tag) r))
             (is (= #{e-mail} (into #{} (map :e-mail r))))
             (is (every? #(contains? (:tags %) tag) r))
             (is (every? #(not (nil? (:date-added %))) r))))

         (deftest tags-retrieval
           (let [e-mail "bob@example.com"
                 link "http://example.com"
                 tag1 "tag1"
                 tag2 "tag2"
                 tag3 "tag3"
                 _ (db/register-user db/u e-mail)
                 _ (db/add-bookmark db/u e-mail link)
                 _ (db/tag-bookmark db/u e-mail link tag1)
                 _ (db/tag-bookmark db/u e-mail link tag2)
                 _ (db/tag-bookmark db/u e-mail link tag3)
                 r (db/get-tags db/u e-mail)]
             (is (= #{tag1 tag2 tag3} r))))
)

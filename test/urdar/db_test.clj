(ns urdar.db-test
  (:require [urdar.db :as db]
            [clojure.test :refer :all]))

(deftest unregistered-user-check
  (let [e-mail "bob@example.com"]
    (is (not (db/user-registered? e-mail)))))

(deftest user-registration
  (let [e-mail "charlie@example.com"
        r (db/register-user e-mail)]
    (is (db/user-registered? e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

;;; TODO check that all tags of deleted user are also deleted

(deftest user-unregistration
  (let [e-mail "alice@example.com"]
    (db/register-user e-mail)
    (db/unregister-user e-mail)
    (is (not (db/user-registered? e-mail)))))

(deftest user-retrieval
  (let [e-mail "jersey@example.com"
        _ (db/register-user e-mail)
        r (db/get-user e-mail)]
    (is (db/user-registered? e-mail))
    (is (= urdar.db.User (class r)))
    (is (= e-mail (:e-mail r)))
    (is (not (nil? (:date-signed r))))))

(deftest unadded-bookmark-check
  (let [e-mail "tim@example.com"
        link "http://example1.com"]
    (is (not (db/bookmark-exists? e-mail link)))))

(deftest bookmark-addition
  (let [e-mail "alex@example.com"
        link "http://example2.com"
        _ (db/register-user e-mail)
        r (db/add-bookmark e-mail link)]
    (is (db/bookmark-exists? e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest bookmark-removal
  (let [e-mail "dirk@example.com"
        link "http://example3.com"]
    (db/register-user e-mail)
    (db/add-bookmark e-mail link)
    (db/delete-bookmark e-mail link)
    (is (not (db/bookmark-exists? e-mail link)))))

(deftest bookmark-retrieval
  (let [e-mail "bobby@example.com"
        link "http://example4.com"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        r (db/get-bookmark e-mail link)]
    (is (db/bookmark-exists? e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))))

(deftest all-bookmarks-retrieval
  (let [e-mail "mike@example.com"
        link1 "http://mike.page1.example.com"
        link2 "http://mike.page2.example.com"
        link3 "http://mike.page3.example.com"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link1)
        _ (db/add-bookmark e-mail link2)
        _ (db/add-bookmark e-mail link3)
        r (db/get-bookmarks e-mail)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2 link3} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? e-mail (:link %)) r))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest some-bookmarks-retrieval
  (let [e-mail "timmy@example.com"
        link1 "http://timmy.page1.example.com"
        link2 "http://timmy.page2.example.com"
        link3 "http://timmy.page3.example.com"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link1)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark e-mail link2)
        _ (Thread/sleep 1000)
        _ (db/add-bookmark e-mail link3)
        r (db/get-bookmarks e-mail 1 2)]
    (is (every? #(= urdar.db.Bookmark (class %)) r))
    (is (= #{link1 link2} (into #{} (map :link r))))
    (is (every? #(db/bookmark-exists? e-mail (:link %)) r))
    (is (every? #(not (nil? (:date-added %))) r))))

(deftest user-unregistration-with-bookmarks
  (let [e-mail "kyle@example.com"
        link1 "http://kyle.page1.example.com"
        link2 "http://kyle.page2.example.com"]
    (db/register-user e-mail)
    (db/add-bookmark e-mail link1)
    (db/add-bookmark e-mail link2)
    (db/unregister-user e-mail)
    (is (not (db/user-registered? e-mail)))
    (is (not (db/bookmark-exists? e-mail link1)))
    (is (not (db/bookmark-exists? e-mail link2)))))

(deftest bookmark-update-description
  (let [e-mail "connor@example.com"
        link "http://connor.example.com"
        desc "Long description."
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        _ (db/update-bookmark e-mail link desc)
        r (db/get-bookmark e-mail link)]
    (is (db/bookmark-exists? e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= link (:link r)))
    (is (not (nil? (:date-added r))))
    (is (nil? (:title r)))
    (is (= desc (:note r)))))

(deftest bookmark-tagging
  (let [e-mail "buffy@example.com"
        link "http://buffy.example.com"
        tag "buffy.tag"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        _ (db/tag-bookmark e-mail link tag)
        r (db/get-bookmark e-mail link)]
    (is (db/bookmark-tagged? e-mail link tag))
    (is (= urdar.db.Bookmark (class r)))
    (is (= #{tag} (into #{} (:tags r))))))

(deftest bookmark-untagging
  (let [e-mail "ramires@example.com"
        link "http://ramires.example.com"
        tag "ramires.tag"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        _ (db/tag-bookmark e-mail link tag)
        _ (db/untag-bookmark e-mail link tag)
        r (db/get-bookmark e-mail link)]
    (is (not (db/bookmark-tagged? e-mail link tag)))
    (is (not ((into #{} (:tags r)) tag)))))

(deftest bookmark-with-tags-retrieval
  (let [e-mail "grace@example.com"
        link "http://grace.example4.com"
        tag1 "grace.tag1"
        tag2 "grace.tag2"
        tag3 "grace.tag3"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        _ (db/tag-bookmark e-mail link tag1)
        _ (db/tag-bookmark e-mail link tag2)
        _ (db/tag-bookmark e-mail link tag3)
        r (db/get-bookmark e-mail link)]
    (is (db/bookmark-exists? e-mail link))
    (is (= urdar.db.Bookmark (class r)))
    (is (= link (:link r)))
    (is (= #{tag1 tag2 tag3} (into #{} (:tags r))))
    (is (not (nil? (:date-added r))))))

(deftest tags-retrieval
  (let [e-mail "kirk@example.com"
        link "http://kirk.example.com"
        tag1 "kirk.tag1"
        tag2 "kirk.tag2"
        tag3 "kirk.tag3"
        _ (db/register-user e-mail)
        _ (db/add-bookmark e-mail link)
        _ (db/tag-bookmark e-mail link tag1)
        _ (db/tag-bookmark e-mail link tag2)
        _ (db/tag-bookmark e-mail link tag3)
        r (db/get-tags e-mail)]
    (is (= #{tag1 tag2 tag3} (into #{} r)))))

(deftest bookmarks-with-tags-removal
  (let [e-mail "john@example.com"
        link1 "http://john1.example.com"
        link2 "http://john2.example.com"
        link3 "http://john3.example.com"
        tag1 "john.tag1"
        tag2 "john.tag2"
        tag3 "john.tag3"]
     (db/register-user e-mail)
     (db/add-bookmark e-mail link1)
     (db/add-bookmark e-mail link2)
     (db/add-bookmark e-mail link3)
     (db/tag-bookmark e-mail link1 tag1)
     (db/tag-bookmark e-mail link1 tag2)
     (db/tag-bookmark e-mail link2 tag2)
     (db/tag-bookmark e-mail link3 tag2)
     (db/tag-bookmark e-mail link3 tag3)
     (is (= #{tag1 tag2 tag3} (into #{} (db/get-tags e-mail))))
     (db/delete-bookmark e-mail link3)
     (is (= #{tag1 tag2} (into #{} (db/get-tags e-mail))))
     (db/delete-bookmark e-mail link1)
     (is (= #{tag2} (into #{} (db/get-tags e-mail))))
     (db/delete-bookmark e-mail link2)
     (is (= #{} (into #{} (db/get-tags e-mail))))))

(comment (deftest all-tagged-bookmarks-retrieval
   (let [e-mail "connel@example.com"
         link1 "http://connel.page1.example.com"
         link2 "http://connel.page2.example.com"
         link3 "http://connel.page3.example.com"
         tag "connel.tag"
         _ (db/register-user e-mail)
         _ (db/add-bookmark e-mail link1)
         _ (db/add-bookmark e-mail link2)
         _ (db/add-bookmark e-mail link3)
         _ (db/tag-bookmark e-mail link1 tag)
         _ (db/tag-bookmark e-mail link2 tag)
         _ (db/tag-bookmark e-mail link3 tag)
         r (db/get-tagged-bookmarks e-mail tag)]
     (is (every? #(= urdar.db.Bookmark (class %)) r))
     (is (= #{link1 link2 link3} (into #{} (map :link r))))
     (is (every? #(db/bookmark-tagged? e-mail (:link %) tag) r))
     (is (every? #((into #{} (:tags %)) tag) r))
     (is (every? #(not (nil? (:date-added %))) r))))

         (deftest some-tagged-bookmarks-retrieval
           (let [e-mail "cyrus@example.com"
                 link1 "http://cyrus.page1.example.com"
                 link2 "http://cyrus.page2.example.com"
                 link3 "http://cyrus.page3.example.com"
                 tag "cyrus.tag"
                 _ (db/register-user e-mail)
                 _ (db/add-bookmark e-mail link1)
                 _ (Thread/sleep 1000)
                 _ (db/add-bookmark e-mail link2)
                 _ (Thread/sleep 1000)
                 _ (db/add-bookmark e-mail link3)
                 _ (db/tag-bookmark e-mail link1 tag)
                 _ (db/tag-bookmark e-mail link2 tag)
                 _ (db/tag-bookmark e-mail link3 tag)
                 r (db/get-tagged-bookmarks e-mail tag 1 2)]
             (is (every? #(= urdar.db.Bookmark (class %)) r))
             (is (= #{link1 link2} (into #{} (map :link r))))
             (is (every? #(db/bookmark-tagged? e-mail (:link %) tag) r))
             (is (every? #((into #{} (:tags %)) tag) r))
             (is (every? #(not (nil? (:date-added %))) r))))

         (deftest recommendations-test
           (let [n 8
                 randomness-factor 0.25
                 e-mail1 "oz@example.com"
                 e-mail2 "el@example.com"
                 e-mail3 "to@example.com"
                 link1 "http://oz.page1.example.com"
                 link2 "http://oz.page2.example.com"
                 link3 "http://oz.page3.example.com"
                 link4 "http://el.page1.example.com"
                 link5 "http://el.page2.example.com"
                 link6 "http://el.page3.example.com"
                 link7 "http://to.page1.example.com"
                 link8 "http://to.page2.example.com"
                 link9 "http://to.page3.example.com"
                 link10 "http://oz.el.page1.example.com"
                 link11 "http://oz.el.page2.example.com"
                 link12 "http://el.to.page1.example.com"
                 link13 "http://el.to.page2.example.com"
                 link14 "http://el.to.page3.example.com"
                 _ (db/register-user e-mail1)
                 _ (db/register-user e-mail2)
                 _ (db/register-user e-mail3)
                 _ (db/add-bookmark e-mail1 link1)
                 _ (db/add-bookmark e-mail1 link2)
                 _ (db/add-bookmark e-mail1 link3)
                 _ (db/add-bookmark e-mail2 link4)
                 _ (db/add-bookmark e-mail2 link5)
                 _ (db/add-bookmark e-mail2 link6)
                 _ (db/add-bookmark e-mail3 link7)
                 _ (db/add-bookmark e-mail3 link8)
                 _ (db/add-bookmark e-mail3 link9)
                 _ (db/add-bookmark e-mail1 link10)
                 _ (db/add-bookmark e-mail1 link11)
                 _ (db/add-bookmark e-mail2 link10)
                 _ (db/add-bookmark e-mail2 link11)
                 _ (db/add-bookmark e-mail2 link12)
                 _ (db/add-bookmark e-mail2 link13)
                 _ (db/add-bookmark e-mail2 link14)
                 _ (db/add-bookmark e-mail3 link12)
                 _ (db/add-bookmark e-mail3 link13)
                 _ (db/add-bookmark e-mail3 link14)
                 _ (Thread/sleep 500)
                 r1 (db/recommend-bookmarks-i db/u randomness-factor n e-mail1)
                 r2 (db/recommend-bookmarks-i db/u randomness-factor n e-mail2)
                 r3 (db/recommend-bookmarks-i db/u randomness-factor n e-mail3)]
             (is (= n (count r1)))
             (is (= n (count r2)))
             (is (= n (count r3)))
             (is (every? (complement (partial db/bookmark-exists? e-mail1)) r1))
             (is (every? (complement (partial db/bookmark-exists? e-mail2)) r2))
             (is (every? (complement (partial db/bookmark-exists? e-mail3)) r3))
             (is (every? (into #{} (map :url r1))
                         [link4 link5 link6 link12 link13 link14]))
             (is (every? (into #{} (map :url r3)) [link4 link5 link6 link10 link11]))
             (is (every? (into #{} (map :url r2))
                         [link1 link2 link3 link7 link8 link9])))))

(deftest link-title
  (let [e-mail "collins@example.com"
        link1 "http://collins1.example.com"
        link2 "http://collins.example.com"]
     (db/register-user e-mail)
     (db/add-bookmark e-mail link1)
     (db/add-bookmark e-mail link2 link2)
     (is (= nil (db/get-title link1)))
     (is (= link2 (db/get-title link2)))))

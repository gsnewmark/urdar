(ns urdar.search-test
  (:require [urdar.search :as s]
            [clojure.test :refer :all]))

(use-fixtures :once (fn [t] (s/init-connection) (t)))

(deftest indexing-and-id-retrieval
  (let [e-mail "jil@example.com"
        link "http://jil.example.com"
        title "Jil's title"
        note "Note"
        doc (s/->BookmarkDoc e-mail link title note)]
    (is (:_ok (s/index-bookmark doc)))
    (is (s/find-bookmark-id e-mail link))))

(deftest unindexing
  (let [e-mail "jude@example.com"
        link1 "http://jude1.example.com"
        link2 "http://jude2.example.com"
        title "Jude's title"
        note "Note"
        doc1 (s/->BookmarkDoc e-mail link1 title note)
        doc2 (s/->BookmarkDoc e-mail link2 title note)]
    (s/index-bookmark doc1)
    (s/index-bookmark doc2)
    (s/unindex-bookmark e-mail link1)
    (is (not (s/find-bookmark-id e-mail link1)))
    (is (s/find-bookmark-id e-mail link2))))

(deftest all-users-bookmarks-unindexing
  (let [e-mail "jovi@example.com"
        link1 "http://jovi1.example.com"
        link2 "http://jovi2.example.com"
        title "Jovi's title"
        note "Note"
        doc1 (s/->BookmarkDoc e-mail link1 title note)
        doc2 (s/->BookmarkDoc e-mail link2 title note)]
    (s/index-bookmark doc1)
    (s/index-bookmark doc2)
    (s/unindex-all-bookmarks e-mail)
    (is (not (s/find-bookmark-id e-mail link1)))
    (is (not (s/find-bookmark-id e-mail link2)))))

(deftest searching
  (let [e-mail "cruz@example.com"
        link1 "http://cruz1.example.com"
        link2 "http://cruz2.example.com"
        title1 "Test"
        title2 "Epic"
        note1 "My small note"
        note2 "My epic very long note that describes everything (including 42)"
        doc1 (s/->BookmarkDoc e-mail link1 title1 note1)
        doc2 (s/->BookmarkDoc e-mail link2 title2 note2)
        s1 (s/find-bookmarks 0 2 e-mail "My long note")
        s2 (s/find-bookmarks 0 1 e-mail "My long note")
        s3 (s/find-bookmarks 1 1 e-mail "My long note")]
    (is (= 2 (count s1)))
    (is (= #{link1 link2} (into #{} (map :link s1))))
    (is (= #{title1 title2} (into #{} (map :title s1))))
    (is (= 1 (count s2)))
    (is (= link2 (:link (first s2))))
    (is (= title2 (:title (first s2))))
    (is (= 1 (count s3)))
    (is (= link1 (:link (first s3))))
    (is (= title1 (:title (first s3))))))

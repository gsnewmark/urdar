(ns urdar.search-test
  (:require [urdar.search :as s]
            [clojure.test :refer :all]
            [clojurewerkz.elastisch.rest.index :as esi]))

(use-fixtures :once (fn [t] (s/init-connection) (esi/refresh) (t)))

(deftest indexing-and-id-retrieval
  (let [e-mail "jil@example.com"
        link "http://jil.example.com"
        title "Jil's title"
        note "Note"
        doc (s/->BookmarkDoc e-mail link title note)]
    (is (:ok (s/index-bookmark doc)))
    (esi/refresh)
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
    (esi/refresh)
    (is (:ok (s/unindex-bookmark e-mail link1)))
    (esi/refresh)
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
    (esi/refresh)
    (is (:ok (s/unindex-all-bookmarks e-mail)))
    (esi/refresh)
    (is (not (s/find-bookmark-id e-mail link1)))
    (is (not (s/find-bookmark-id e-mail link2)))))

(deftest small-searching
  (let [e-mail "cruz@example.com"
        link1 "http://cruz1.example.com"
        link2 "http://cruz2.example.com"
        title1 "Test"
        title2 "Epic"
        note1 "My small note"
        note2 "My epic very long note that describes everything (including 42)"
        doc1 (s/->BookmarkDoc e-mail link1 title1 note1)
        doc2 (s/->BookmarkDoc e-mail link2 title2 note2)
        doc3 (s/->BookmarkDoc "cds@example.com" "http://cds.example.com"
                              "cds" "cds desc")
        _ (s/index-bookmark doc1)
        _ (s/index-bookmark doc2)
        _ (s/index-bookmark doc3)
        _ (esi/refresh)
        query "My epic long note"
        s1 (s/find-bookmarks 0 2 e-mail query)
        s2 (s/find-bookmarks 0 1 e-mail query)
        s3 (s/find-bookmarks 1 1 e-mail query)]
    (is (= 2 (count s1)))
    (is (= #{link1 link2} (into #{} s1)))
    (is (= 1 (count s2)))
    (is (= link2 (first s2)))
    (is (= 1 (count s3)))
    (is (= link1 (first s3)))))

(deftest bigger-searching
  (let [e-mail "jane@example.com"
        link1 "http://jane1.example.com"
        link2 "http://jane2.example.com"
        link3 "http://jane3.example.com"
        link4 "http://jane4.example.com"
        link5 "http://jane5.example.com"
        link6 "http://jane6.example.com"
        link7 "http://jane7.example.com"
        link8 "http://jane8.example.com"
        link9 "http://jane9.example.com"
        link10 "http://jane10.example.com"
        title7 "Seventh title"
        note7 "Some note about seventh title"
        rand-str (fn [n]
                   (apply
                    str
                    (repeatedly n #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))
        doc1 (s/->BookmarkDoc e-mail link1 (rand-str 5) (rand-str 15))
        doc2 (s/->BookmarkDoc e-mail link2 (rand-str 5) (rand-str 15))
        doc3 (s/->BookmarkDoc e-mail link3 (rand-str 5) (rand-str 15))
        doc4 (s/->BookmarkDoc e-mail link4 (rand-str 5) (rand-str 15))
        doc5 (s/->BookmarkDoc e-mail link5 (rand-str 5) (rand-str 15))
        doc6 (s/->BookmarkDoc e-mail link6 (rand-str 5) (rand-str 15))
        doc7 (s/->BookmarkDoc e-mail link7 title7 note7)
        doc8 (s/->BookmarkDoc e-mail link8 (rand-str 5) (rand-str 15))
        doc9 (s/->BookmarkDoc e-mail link9 (rand-str 5) (rand-str 15))
        doc10 (s/->BookmarkDoc e-mail link10 (rand-str 5) (rand-str 15))
        docs [doc1 doc2 doc3 doc4 doc5 doc6 doc7 doc8 doc9 doc10]
        _ (doseq [d docs] (s/index-bookmark d))
        _ (esi/refresh)
        query "Seventh note"
        s (s/find-bookmarks 0 10 e-mail query)]
    (is (= 1 (count s)))
    (is (= link7 (first s)))))

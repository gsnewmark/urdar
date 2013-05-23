(ns urdar.db
  (:require [urdar.db.neo4j :as n4j]
            [urdar.config :as c]
            [clojure.set :as set]))

(defrecord User [e-mail date-signed])
(defrecord Bookmark [link title note tags date-added])
(defrecord Link [url])

(defprotocol UserOperations
  "DB operations that could be performed during ordinary user's usage of
  application."
  (init-connection-i [self]
    "Performs required initialization of database connection and returns it.")
  (user-registered-i? [self e-mail]
    "Checks whether the user with given e-mail exists in database.")
  (register-user-i [self e-mail]
    "Creates user with given e-mail in DB. Returns instance of User.")
  (unregister-user-i [self e-mail]
    "Deletes user with given e-mail along with all theirs stored bookmarks
    from database.")
  (get-user-i [self e-mail] "Retrieves User instance if user is registered.")
  (bookmark-exists-i? [self e-mail link]
    "Checks whether given link is already bookmarked by user.")
  (add-bookmark-i [self e-mail link title] [self e-mail link]
    "Bookmarks a given link (probably, creating it in DB) for the given user.
    Returns Bookmark instance.")
  (delete-bookmark-i [self e-mail link]
    "Deletes given link from user's bookmarks (if it's present). If no other
    bookmark is tagged by some tags of this bookmark, remove these tags as
    well.")
  (get-bookmark-i [self e-mail link]
    "Retrieves instance of Bookmark if user already saved it.")
  (get-bookmarks-i [self e-mail] [self e-mail skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) of
    given user sorted by their creation date (descending), optionally
    skipping first skip bookmarks.")
  (update-bookmark-i [self e-mail link note]
    "Adds a note to a given bookmark.")
  (get-title-i [self link]
    "Retrieves a title for the given link if it is presented in DB.")
  (bookmark-tagged-i? [self e-mail link tag]
    "Checks whether given bookmark has given tag.")
  (tag-bookmark-i [self e-mail link tag]
    "Adds tag to given bookmark.")
  (untag-bookmark-i [self e-mail link tag]
    "Removes tag from given bookmark. If no other bookmark is tagged by this
    tag, also removes tag itself.")
  (get-tagged-bookmarks-i [self e-mail tag] [self e-mail tag skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) with
    given tag of given user sorted by their creation date (descending),
    optionally skipping first skip bookmarks.")
  (get-tags-i [self e-mail] "Returns a set of all tags of given user.")
  (recommend-bookmarks-i [self randomness-factor n e-mail]
    "Returns top n link recommendations for user (list of instances of
    Link). Randomness factor is float [0,1] that specifies what percent of
    results are random links."))

;;; TODO maybe use constructor from map
(defn- node->User
  [node]
  (when-let [{:keys [e-mail date-signed]} (:data node)]
    (->User e-mail date-signed)))

(defn- node->Bookmark
  [link title tags node]
  (when-let [{:keys [note date-added]} (:data node)]
    (->Bookmark link title note tags date-added)))

(defn- cypher-bookmark-result->Bookmark
  [res]
  (let [{link "l.url" title "l.title?" note "b.note?"
         tags "COLLECT(DISTINCT t.tag?)"
         date-added "b.date-added" :as b} res]
    (->Bookmark link title note tags date-added)))

(defrecord UserOperationsNeo4j [url login password]
  UserOperations
  (init-connection-i [self] (n4j/init-connection url login password) self)
  (user-registered-i? [_ e-mail] (not (nil? (n4j/get-user-node e-mail))))
  (register-user-i [_ e-mail]
    (when-let [node (n4j/create-user-node e-mail)] (node->User node)))
  (unregister-user-i [_ e-mail]
    (when-let [node (n4j/get-user-node e-mail)] (n4j/delete-user-node node)))
  (get-user-i [_ e-mail]
    (when-let [node (n4j/get-user-node e-mail)] (node->User node)))
  (bookmark-exists-i? [_ e-mail link]
    (not (nil? (n4j/get-bookmark-node e-mail link))))
  (add-bookmark-i [self e-mail link] (add-bookmark-i self e-mail link nil))
  (add-bookmark-i [_ e-mail link title]
    (let [user-node (n4j/get-user-node e-mail)
          link-node (or (n4j/get-link-node link)
                        (n4j/create-link-node link title))]
      (when (and user-node link-node)
        (some->> (n4j/create-bookmark-node user-node link-node)
                 (node->Bookmark link title [])))))
  (delete-bookmark-i [_ e-mail link]
    (when-let [node (n4j/get-bookmark-node e-mail link)]
      (n4j/delete-bookmark-node e-mail link node)))
  (get-bookmark-i [self e-mail link]
    (when-let [node (n4j/get-bookmark-node e-mail link)]
      (let [title (get-title-i self link)]
       (node->Bookmark link title (n4j/get-tags-for-bookmark e-mail link)
                       node))))
  (get-bookmarks-i [self e-mail] (get-bookmarks-i self e-mail nil nil))
  (get-bookmarks-i [_ e-mail skip quant]
    (map cypher-bookmark-result->Bookmark
         (n4j/get-bookmarks-for-user e-mail [skip quant])))
  (update-bookmark-i
    [_ e-mail link note]
    (let [bookmark-node (n4j/get-bookmark-node e-mail link)
          data (-> {} (#(if note (assoc % :note note) %)))]
      (n4j/update-node bookmark-node data)))
  (get-title-i [_ link] (n4j/get-link-title link))
  (bookmark-tagged-i? [_ e-mail link tag]
    (not (nil? (n4j/get-tag-rel e-mail link tag))))
  (tag-bookmark-i [_ e-mail link tag]
    (let [user-node (n4j/get-user-node e-mail)
          bookmark-node (n4j/get-bookmark-node e-mail link)
          tag-rel (n4j/get-tag-rel e-mail link tag)]
      (when (and user-node bookmark-node (nil? tag-rel))
        (n4j/tag-bookmark user-node bookmark-node link tag))))
  (untag-bookmark-i [_ e-mail link tag]
    (let [tag-rel (n4j/get-tag-rel e-mail link tag)]
      (n4j/untag-bookmark tag-rel)))
  (get-tagged-bookmarks-i [self e-mail tag]
    (get-tagged-bookmarks-i self e-mail tag nil nil))
  (get-tagged-bookmarks-i [_ e-mail tag skip quant]
    (map cypher-bookmark-result->Bookmark
         (n4j/get-tagged-bookmarks-for-user e-mail tag [skip quant])))
  (get-tags-i [_ e-mail] (n4j/get-tags-for-user e-mail))
  (recommend-bookmarks-i [_ randomness-factor n e-mail]
    (let [random-bookmarks
          (into #{} (map #(hash-map :url (get % "l.url")
                                    :title (get % "l.title?"))
                         (n4j/random-bookmarks-for-user n e-mail)))

          recommended-bookmarks
          (into #{} (map #(hash-map :url (get % "url")
                                    :title (get % "title"))
                         (n4j/recommended-bookmarks-for-user n e-mail)))

          number-of-recommended (count recommended-bookmarks)
          number-of-recommended-to-show (int (* n (- 1.0 randomness-factor)))

          number-of-recommended-to-show
          (if (> number-of-recommended-to-show number-of-recommended)
            number-of-recommended
            number-of-recommended-to-show)

          number-of-random-to-show (- n number-of-recommended-to-show)

          random-nonrecommended-bookmarks
          (set/difference random-bookmarks recommended-bookmarks)

          recommendations
          (shuffle
           (concat
            (take number-of-recommended-to-show
                  (shuffle recommended-bookmarks))
            (take number-of-random-to-show
                  (shuffle random-nonrecommended-bookmarks))))]
      recommendations)))

(def u
  (let [{:keys [url login password]} (:neo4j c/config)]
    (-> (->UserOperationsNeo4j url login password)
        init-connection-i)))

(def user-registered? (partial user-registered-i? u))
(def register-user (partial register-user-i u))
(def get-user (partial get-user-i u))
(def unregister-user (partial unregister-user-i u))
(def get-bookmarks (partial get-bookmarks-i u))
(def get-bookmark (partial get-bookmark-i u))
(def update-bookmark (partial update-bookmark-i u))
(def get-tags (partial get-tags-i u))
(def get-tagged-bookmarks (partial get-tagged-bookmarks-i u))
(def bookmark-exists? (partial bookmark-exists-i? u))
(def add-bookmark (partial add-bookmark-i u))
(def delete-bookmark (partial delete-bookmark-i u))
(def tag-bookmark (partial tag-bookmark-i u))
(def untag-bookmark (partial untag-bookmark-i u))
(def bookmark-tagged? (partial bookmark-tagged-i? u))
(def recommend-bookmarks (partial recommend-bookmarks-i u 0.3 10))
(def get-title (partial get-title-i u))

(ns urdar.db)

(defrecord User [e-mail date-signed])
(defrecord Bookmark [e-mail link title note tags date-added])

(defprotocol UserOperations
  (init-connection [self]
    "Performs required initialization of database connection and returns it.")
  (user-registered? [self e-mail]
    "Checks whether the user with given e-mail exists in database.")
  (register-user [self e-mail]
    "Creates user with given e-mail in DB. Returns instance of User.")
  (unregister-user [self e-mail]
    "Deletes user with given e-mail along with all theirs stored bookmarks
    from database.")
  (get-user [self e-mail] "Retrieves User instance if user is registered.")
  (bookmark-exists? [self e-mail link]
    "Checks whether given link is already bookmarked by user.")
  (add-bookmark [self e-mail link]
    "Bookmarks a given link (probably, creating it in DB) for the given user.
    Returns Bookmark instance.")
  (delete-bookmark [self e-mail link]
    "Deletes given link from user's bookmarks (if it's present).")
  (get-bookmark [self e-mail link]
    "Retrieves instance of Bookmark if user already saved it.")
  (get-bookmarks [self e-mail] [self e-mail skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) of
    given user sorted by their creation date (descending), optionally
    skipping first skip bookmarks.")
  (update-bookmark [self e-mail link & {:keys [title description]}]
    "Adds a title and/or description to a given bookmark. Returns instance
    of Bookmark.")
  (bookmark-tagged? [self e-mail link tag]
    "Checks whether given bookmark has given tag.")
  (tag-bookmark [self e-mail link tag]
    "Adds tag to given bookmark. Returns instance of Bookmark.")
  (untag-bookmark [self e-mail link tag]
    "Removes tag from given bookmark. If no other bookmark is tagged by this
    tag, also removes tag itself.")
  (get-tagged-bookmarks [self e-mail tag] [self e-mail tag skip quant]
    "Retrieves all or quant number of bookmarks (instances of Bookmark) with
    given tag of given user sorted by their creation date (descending),
    optionally skipping first skip bookmarks.")
  (tag-exists? [self e-mail tag]
    "Checks whether given tag exists for a given user.")
  (get-tags [self e-mail] "Returns a set of all tags of given user."))

(def u)

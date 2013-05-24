(ns urdar.client.reactions
  (:require [urdar.client.dom :as d]
            [urdar.client.remote :as r]
            [urdar.client.signal :as s]
            [urdar.client.state :as st]
            [enfocus.core :as ef]))

(defprotocol Reaction
  (react [signal] "Reacts to the given signal."))

(defn activate-reactions
  []
  (s/add-reactor react)
  (extend-protocol Reaction
    s/TagFilterChangedSignal
    (react [signal]
      (let [{:keys [tag]} signal]
        (st/set-tag! tag)
        (d/remove-all-bookmarks)
        ;; TODO fetches two times if horizontal space is small
        (r/fetch-bookmarks)
        (d/tag-filter-selected tag)))
    s/RecommendationReceivedSignal
    (react [signal]
      (let [{:keys [links]} signal]
        (d/render-recommendations-list links)))
    s/TagRemovedSignal
    (react [signal]
      (let [{:keys [tag node]} signal]
        (d/remove-tag-node node tag)
        (r/fetch-tags true)
        (st/unset-tag! tag)))
    s/RenderTagSignal
    (react [signal]
      (let [{:keys [node link tag]} signal]
        (d/render-tag node link tag)))
    s/TagAddedSignal
    (react [signal]
      (let [{:keys [node link tag update-tag-menu?]} signal]
        (s/signal (s/->RenderTagSignal node link tag))
        (r/fetch-tags update-tag-menu?)
        (d/new-tag-validation-succeeded node)
        (d/clear-input-element ".new-tag" node)))
    s/TagMenuChangedSignal
    (react [signal]
      (let [{:keys [tag reset-menu?]} signal]
        (d/clean-tags-menu reset-menu?)
        (d/render-tag-menu-element tag)))
    s/BookmarkRemovedSignal
    (react [signal]
      (let [{:keys [node update-tag-menu?]} signal]
        (d/remove-node node)
        (st/bookmark-removed!)
        (r/fetch-tags update-tag-menu?)
        ;; TODO
        ;(s/unset-tag! tag)
        ))
    s/BookmarkAddedSignal
    (react [signal]
      (let [{:keys [link new? tags title note]} signal]
        (when new? (st/set-query! nil))
        (st/bookmark-fetched!)
        (d/new-link-validation-succeeded)
        (d/render-bookmark link new? tags title note)
        (d/clear-input-element "#link-to-add")))
    s/NewLinkValidationFailed
    (react [signal]
      (let [{:keys [msg]} signal]
        (d/new-link-validation-failed msg)))
    s/NewTagValidationFailed
    (react [signal]
      (let [{:keys [node msg]} signal]
        (d/new-tag-validation-failed node msg)))
    s/NoteChangeSignal
    (react [signal]
      (let [{:keys [node to-edit? save? link note]} signal]
        (when save? (r/add-note! link note))
        (d/render-note node to-edit? link note)))
    s/SearchSignal
    (react [signal]
      (let [{:keys [query]} signal]
        (when-not (empty? query)
          (st/set-query! query)
          (s/signal (s/->TagFilterChangedSignal nil))
          (d/unselect-tag-filters))))))

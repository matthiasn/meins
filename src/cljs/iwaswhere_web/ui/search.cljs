(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [iwaswhere-web.ui.draft :as d]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.string :as s]
            [clojure.set :as set]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]))

(defn tags-view
  "Renders a row with tags, if any in current query."
  [current-query]
  (let [get-tags #(% current-query)
        tags (get-tags :tags)
        not-tags (get-tags :not-tags)
        mentions (get-tags :mentions)]
    (when (or (seq tags) (seq not-tags) (seq mentions))
      [:div.hashtags
       (for [tag tags]
         ^{:key (str "search-" tag)}
         [:span.hashtag tag])
       (for [tag not-tags]
         ^{:key (str "search-n" tag)}
         [:span.hashtag.not-tag tag])
       (for [tag mentions]
         ^{:key (str "search-" tag)}
         [:span.mention tag])])))

(defn editor-state
  "Create editor-state, either from deserialized state or from search string."
  [q]
  (if-let [editor-state (:editor-state q)]
    (d/editor-state-from-raw (clj->js editor-state))
    (d/editor-state-from-text (or (:search-text q) ""))))

(defn search-field-view
  "Renders search field for current tab."
  [query-id put-fn]
  (let [query-cfg (subscribe [:query-cfg])
        cfg (subscribe [:cfg])
        options (subscribe [:options])
        stories (reaction (:stories @options))]
    (fn [query-id put-fn]
      (let [query (query-id (:queries @query-cfg))
            editor-state2 (d/editor-state-from-text "")
            story-name (get-in @stories [:story-name] "test: story")
            editor-state2 (d/editor-state-from-text story-name)
            search-send (fn [text editor-state]
                          (let [query (query-id (:queries @query-cfg))
                                path [:entityMap :0 :data :mention :id]
                                story (get-in editor-state path)
                                story (when story (js/parseInt story))
                                story (->> editor-state
                                           :entityMap
                                           vals
                                           (filter #(= (:type %) "$mention"))
                                           first
                                           :data
                                           :mention
                                           :id)
                                story (when story (js/parseInt story))
                                _ (prn story editor-state)
                                s (merge query
                                         (p/parse-search text)
                                         {:story        story
                                          :editor-state editor-state})]
                            (put-fn [:search/update s])))
            story-select-cb (fn [text editor-state]
                              (prn editor-state)
                              (let [path [:entityMap :0 :data :mention :id]
                                    story (get-in editor-state path)
                                    story (when story (js/parseInt story))
                                    q (merge query {:story story})]
                                (prn q)
                                (put-fn [:search/update q])))
            select-story (fn [_story])]
        (when-not (:briefing query)
          [:div.search
           [tags-view query]
           ^{:key query-id}
           [:div.search-row
            [d/draft-search-field (editor-state query) search-send select-story]
            [d/story-search-field editor-state2 story-select-cb select-story]]])))))

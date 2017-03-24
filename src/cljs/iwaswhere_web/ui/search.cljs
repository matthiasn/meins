(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [iwaswhere-web.ui.draft :as draft]
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
    (draft/editor-state-from-raw (clj->js editor-state))
    (draft/editor-state-from-text (or (:search-text q) ""))))

(defn search-field-view
  "Renders search field for current tab."
  [query-id put-fn]
  (let [query-cfg (subscribe [:query-cfg])
        cfg (subscribe [:cfg])
        options (subscribe [:options])
        mentions (reaction (map (fn [m] {:name m}) (:mentions @options)))
        hashtags (reaction
                   (let [show-pvt? (:show-pvt @cfg)
                         hashtags (:hashtags @options)
                         pvt-hashtags (:pvt-hashtags @options)
                         hashtags (if show-pvt?
                                    (concat hashtags pvt-hashtags)
                                    hashtags)]
                     (map (fn [h] {:name h}) hashtags)))]
    (fn [query-id put-fn]
      (let [query (query-id (:queries @query-cfg))
            search-send (fn [text editor-state]
                          (let [query (query-id (:queries @query-cfg))
                                s (merge query
                                         (p/parse-search text)
                                         {:editor-state editor-state})]
                            (put-fn [:search/update s])))
            story-select-handler
            (fn [ev]
              (let [v (-> ev .-nativeEvent .-target .-value)
                    story (js/parseInt v)
                    q (merge query {:story (when-not (js/isNaN story) story)})]
                (put-fn [:search/update q])))]
        (when-not (:briefing query)
          [:div.search
           [tags-view query]
           ^{:key query-id}
           [:div.search-row
            [draft/draft-search-field
             (editor-state query) search-send @mentions @hashtags]
            [:select {:value     (or (:story query) "")
                      :on-change story-select-handler}
             [:option {:value ""} "no story selected"]
             (for [[id story] (:sorted-stories @options)]
               (let [story-name (:story-name story)]
                 ^{:key (str query-id id story-name)}
                 [:option {:value id} story-name]))]]])))))

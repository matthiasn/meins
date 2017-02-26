(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [iwaswhere-web.ui.draft :as draft]
            [clojure.string :as s]
            [clojure.set :as set]
            [re-frame.core :refer [reg-event-db path reg-sub dispatch
                                   dispatch-sync subscribe]]
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
        state (r/atom {:local-query (query-id (:queries @query-cfg))
                       :query-id    query-id
                       :focused     true})
        q (query-id (:queries @query-cfg))
        search-editor-state (editor-state q)]
    (fn [query-id put-fn]
      (let [current-query (query-id (:queries @query-cfg))

            search-send (fn [text editor-state]
                          (let [s (merge current-query
                                         (p/parse-search text)
                                         {:editor-state editor-state})]
                            (put-fn [:search/update s])))

            update-search-fn (fn [search-str]
                               (let [s (merge (:local-query @state)
                                              (p/parse-search search-str))]
                                 (put-fn [:search/update s])))

            before-cursor (h/string-before-cursor (:search-text current-query))
            show-pvt? (:show-pvt @cfg)
            hashtags (:hashtags @options)
            pvt-hashtags (:pvt-hashtags @options)
            hashtags (if show-pvt? (concat hashtags pvt-hashtags) hashtags)
            mentions (:mentions @options)
            [curr-tag f-tags] (p/autocomplete-tags before-cursor "#" hashtags)
            [curr-mention f-mentions] (p/autocomplete-tags before-cursor "@" mentions)

            story-select-handler
            (fn [ev]
              (let [v (-> ev .-nativeEvent .-target .-value)
                    story (js/parseInt v)
                    q (merge current-query
                             {:story (when-not (js/isNaN story) story)})]
                (put-fn [:search/update q])))

            ;mentions-list (map (fn [m] {:name (subs m 1)}) mentions)
            ;hashtags-list (map (fn [m] {:name (subs m 1)}) hashtags)

            mentions-list (map (fn [m] {:name m}) mentions)
            hashtags-list (map (fn [h] {:name h}) hashtags)]

        (when (not= (:query-id @state) query-id)
          (let [q (query-id (:queries @query-cfg))
                new-editor-state (editor-state q)]
            (reset! search-editor-state @new-editor-state))
          (swap! state assoc-in [:query-id] query-id))

        [:div.search
         [tags-view current-query]
         (when (seq mentions-list)
           ^{:key query-id}
           [:div.search-row
            [draft/draft-search-field
             search-editor-state search-send mentions-list hashtags-list]
            [:select {:value     (or (:story current-query) "")
                      :on-change story-select-handler}
             [:option {:value ""} "no story selected"]
             (for [[id story] (:sorted-stories @options)]
               (let [story-name (:story-name story)]
                 ^{:key (str query-id id story-name)}
                 [:option {:value id} story-name]))]])]))))

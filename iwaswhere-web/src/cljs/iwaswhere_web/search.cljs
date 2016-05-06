(ns iwaswhere-web.search
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [clojure.string :as s]))

(defn parse-search
  "Parses search string for hashtags, mentions, and hashtags that should not be contained in the filtered entries.
  Such hashtags can for now be marked like this: #~done. Finding tasks that are not done, which don't have #done
  in either the entry or any of its comments, can be found like this: #task #~done"
  [text]
  (let [tags (set (re-seq (js/RegExp. "#[\\w\\-\\u00C0-\\u017F]+" "m") text))
        not-tags (re-seq (js/RegExp. "#~[\\w\\-\\u00C0-\\u017F]+" "m") text)
        mentions (set (re-seq (js/RegExp. "@[\\w\\-\\u00C0-\\u017F]+" "m") text))]
    {:tags     tags
     :not-tags (set (map #(s/replace % #"~" "") not-tags))
     :mentions mentions}))

(defn search-view
  "Renders search component."
  [{:keys [local put-fn]}]
  (let [on-change-fn #(let [query (parse-search (.. % -target -value))]
                       (swap! local assoc-in [:entry] query)
                       (put-fn [:state/get query]))]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:hr]
      (let [tags (:tags (:entry @local))]
        [:div.hashtags
         (when (seq tags)
           (for [hashtag tags]
             ^{:key (str "tag-" hashtag)}
             [:span.hashtag.float-left hashtag]))])
      (let [tags (:not-tags (:entry @local))]
        [:div.hashtags
         (when (seq tags)
           (for [hashtag tags]
             ^{:key (str "tag-" hashtag)}
             [:span.hashtag.float-left.not-tag hashtag]))])
      [:div.textentry
       [:textarea {:type "text" :on-change on-change-fn}]]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn search-view
              :dom-id  "search"}))

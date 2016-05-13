(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn tags
  "Renders horizontal list of tags."
  [store-snapshot k css-class]
  (let [tags (k (:current-query store-snapshot))]
    [:div.hashtags (when (seq tags)
                     (for [tag tags]
                       ^{:key (str css-class "-" tag)}
                       [:span.float-left {:class css-class} tag]))]))

(defn search-view
  "Renders search component."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        on-change-fn #(put-fn [:state/get (h/parse-search (.. % -target -value))])]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [tags store-snapshot :tags "hashtag"]
      [tags store-snapshot :not-tags "hashtag not-tag"]
      [tags store-snapshot :mentions "mention"]
      [:div.textentry
       [:textarea {:type      "text"
                   :on-change on-change-fn
                   :value     (:search-text (:current-query store-snapshot))}]]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn search-view
              :dom-id  "search"}))

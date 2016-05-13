(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn search-view
  "Renders search component."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        on-change-fn #(put-fn [:state/get (h/parse-search (.. % -target -value))])]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      (let [tags (:tags (:current-query store-snapshot))]
        [:div.hashtags (when (seq tags)
                         (for [hashtag tags]
                           ^{:key (str "tag-" hashtag)}
                           [:span.hashtag.float-left hashtag]))])
      (let [tags (:not-tags (:current-query store-snapshot))]
        [:div.hashtags (when (seq tags)
                         (for [hashtag tags]
                           ^{:key (str "tag-" hashtag)}
                           [:span.hashtag.float-left.not-tag hashtag]))])
      (let [mentions (:mentions (:current-query store-snapshot))]
        [:div.hashtags (when (seq mentions)
                         (for [mention mentions]
                           ^{:key (str "tag-" mention)}
                           [:span.mention.float-left mention]))])
      [:div.textentry
       [:textarea {:type      "text"
                   :on-change on-change-fn
                   :value     (:search-text (:current-query store-snapshot))}]]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn search-view
              :dom-id  "search"}))

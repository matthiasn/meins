(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn tags
  "Renders horizontal list of tags."
  [state-snapshot k css-class]
  (let [tags (k (:current-query state-snapshot))]
    [:div.hashtags (when (seq tags)
                     (for [tag tags]
                       ^{:key (str css-class "-" tag)}
                       [:span.float-left {:class css-class} tag]))]))

(defn search-view
  "Renders search component."
  [{:keys [observed local put-fn]}]
  (let [store-snapshot @observed
        local-snapshot @local
        on-change-fn #(let [new-search (h/parse-search (.. % -target -value))]
                       (swap! local assoc-in [:current-query] new-search)
                       (aset js/window "location" "hash" (js/encodeURIComponent (:search-text new-search)))
                       (put-fn [:state/get new-search]))]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [tags local-snapshot :tags "hashtag"]
      [tags local-snapshot :not-tags "hashtag not-tag"]
      [tags local-snapshot :mentions "mention"]
      [:div.textentry
       [:textarea {:type      "text"
                   :on-change on-change-fn
                   :value     (:search-text (:current-query local-snapshot))}]]]]))

(defn init-fn
  "Initializes listener for location hash changes, which alters local component state with
  the latest query on change, plus sends query to backend."
  [{:keys [local put-fn]}]
  (let [hash-change-fn #(let [new-query (h/query-from-search-hash)]
                         (when (not= new-query (:current-query @local))
                           (swap! local assoc-in [:current-query] new-query)
                           (put-fn [:state/get new-query])))]
    (aset js/window "onhashchange" hash-change-fn)
    (hash-change-fn)))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :init-fn init-fn
              :view-fn search-view
              :dom-id  "search"}))

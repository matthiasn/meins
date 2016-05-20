(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn tags
  "Renders horizontal list of tags."
  [state-snapshot k css-class]
  (let [tags (k (:current-query state-snapshot))]
    [:div.hashtags (when (seq tags)
                     (for [tag tags]
                       ^{:key (str css-class "-" tag)}
                       [:span.float-left {:class css-class} tag]))]))

(defn cfg-view
  "Renders component for toggling display of maps, comments, ..."
  [local store-snapshot put-fn]
  (let [show-all-maps? (:show-all-maps store-snapshot)
        toggle-all-maps #(put-fn [:cmd/toggle-key {:key :show-all-maps}])
        show-tags? (:show-hashtags store-snapshot)
        toggle-tags #(put-fn [:cmd/toggle-key {:key :show-hashtags}])
        show-context? (:show-context store-snapshot)
        toggle-context #(put-fn [:cmd/toggle-key {:key :show-context}])
        show-pvt? (:show-pvt store-snapshot)
        toggle-pvt #(put-fn [:cmd/toggle-key {:key :show-pvt}])
        sort-by-upvotes? (:sort-by-upvotes store-snapshot)
        toggle-upvotes #(do (put-fn [:cmd/toggle-key {:key :sort-by-upvotes}])
                            (put-fn [:state/get (merge (:current-query @local)
                                                       {:sort-by-upvotes (not sort-by-upvotes?)})]))
        show-comments? (:show-comments store-snapshot)
        toggle-comments #(put-fn [:cmd/toggle-key {:key :show-comments}])]
    [:div.pure-u-1.toggle-cmds
     [:span.fa.fa-comments.toggle.pull-right.tooltip
      {:class (when-not show-comments? "hidden-comments") :on-click toggle-comments}
      [:span.tooltiptext "show comments"]]
     [:span.fa.toggle.pull-right.tooltip {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}
      [:span.tooltiptext "show all maps"]]
     [:span.fa.fa-hashtag.toggle.pull-right.tooltip {:class (when-not show-tags? "inactive") :on-click toggle-tags}
      [:span.tooltiptext "show hashtag symbol"]]
     [:span.fa.fa-eye.toggle.pull-right.tooltip {:class (when-not show-context? "inactive") :on-click toggle-context}
      [:span.tooltiptext "show query results"]]
     [:span.fa.fa-user-secret.toggle.pull-right.tooltip {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}
      [:span.tooltiptext "show private entries"]]
     [:span.fa.fa-thumbs-up.toggle.pull-right.tooltip
      {:class (when-not sort-by-upvotes? "inactive") :on-click toggle-upvotes}
      [:span.tooltiptext "sort by upvotes first"]]
     [:hr]]))

(defn search-view
  "Renders search component."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        location-timeout-fn (fn [search-text]
                              (.setTimeout js/window
                                           #(aset js/window "location" "hash" (js/encodeURIComponent search-text))
                                           5000))
        on-change-fn #(let [new-search (h/parse-search (.. % -target -value))]
                       (swap! local assoc-in [:current-query] new-search)
                       (swap! local update-in [:set-location] (fn [prev] (when prev (.clearTimeout js/window prev))
                                                                (location-timeout-fn (:search-text new-search))))
                       (put-fn [:state/get (merge new-search {:sort-by-upvotes (:sort-by-upvotes @observed)})]))]
    [:div.l-box-lrg.pure-g.search-div
     [:div.pure-u-1
      [tags local-snapshot :tags "hashtag"]
      [tags local-snapshot :not-tags "hashtag not-tag"]
      [tags local-snapshot :mentions "mention"]
      [:div.textentry
       [:textarea {:type      "text"
                   :on-change on-change-fn
                   :value     (:search-text (:current-query local-snapshot))}]]]
     [:div.pure-u-1
      [:div.entry-footer
       [u/btn-w-tooltip "fa-plus-square" "new" "new entry" (h/new-entry-fn put-fn {}) "pure-button-primary"]
       [u/btn-w-tooltip "fa-camera-retro" "import" "import photos" #(put-fn [:import/photos])]
       [u/btn-w-tooltip "fa-map" "import" "import visits" #(put-fn [:import/geo])]
       [u/btn-w-tooltip "fa-mobile-phone" "import" "import phone entries" #(put-fn [:import/phone])]]]
     [cfg-view local @observed put-fn]]))

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

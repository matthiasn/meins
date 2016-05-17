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
    [:div.pure-u-1
      [:span.fa.fa-comments.toggle-map.pull-right
       {:class (when-not show-comments? "hidden-comments") :on-click toggle-comments}]
      [:span.fa.toggle-map.pull-right {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}]
      [:span.fa.fa-hashtag.toggle-map.pull-right {:class (when-not show-tags? "inactive") :on-click toggle-tags}]
      [:span.fa.fa-eye.toggle-map.pull-right {:class (when-not show-context? "inactive") :on-click toggle-context}]
      [:span.fa.fa-user-secret.toggle-map.pull-right {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}]
      [:span.fa.fa-thumbs-up.toggle-map.pull-right
       {:class (when-not sort-by-upvotes? "inactive") :on-click toggle-upvotes}]
      [:hr]]))

(defn search-view
  "Renders search component."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        on-change-fn #(let [new-search (h/parse-search (.. % -target -value))]
                       (swap! local assoc-in [:current-query] new-search)
                       (aset js/window "location" "hash" (js/encodeURIComponent (:search-text new-search)))
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
       [:button.pure-button.pure-button-primary.button-xsmall {:on-click (h/new-entry-fn put-fn {})}
        [:span.fa.fa-plus-square] " new"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/photos])}
        [:span.fa.fa-camera-retro] " import"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/geo])}
        [:span.fa.fa-map-o] " import"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/phone])}
        [:span.fa.fa-mobile-phone] " import"]]]
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

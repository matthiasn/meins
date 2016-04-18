(ns iwaswhere-web.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.leaflet :as l]
            [iwaswhere-web.markdown :as m]
            [iwaswhere-web.image :as i]
            [cljsjs.moment]))

(defn hashtags-list
  "Horizontally renders hashtags list."
  [entry]
  (let [tags (:tags entry)]
    (when (seq tags)
      [:div.pure-u-1
       [:div.hashtags
        (for [hashtag tags]
          ^{:key (str "tag-" hashtag)}
          [:span.hashtag hashtag])]])))

(defn journal-entry
  "Renders individual journal entry. Interaction with application state happens via
  messages that are sent to the store component, for example for toggling the display
  of the edit mode or showing the map for an entry. The editable content component
  used in edit mode also sends a modified entry to the store component, which is useful
  for displaying updated hashtags, or also for showing the warning that the entry is not
  saved yet."
  [entry temp-entry put-fn show-map? editable? show-all-maps? show-tags?]
  (let [ts (:timestamp entry)
        map? (:latitude entry)
        toggle-map #(put-fn [:cmd/toggle {:ts ts :key :show-maps-for}])
        toggle-edit #(put-fn [:cmd/toggle {:ts ts :key :show-edit-for}])]
    [:div.entry
     [:div.entry-header
      [:span.timestamp (.format (js/moment ts) "MMMM Do YYYY, h:mm:ss a")]
      (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
      [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]
      (when (and temp-entry (not= entry temp-entry))
        [:span.not-saved [:span.fa.fa-exclamation-triangle] " not saved"])]
     [hashtags-list (or temp-entry entry)]
     [l/leaflet-map entry (or show-map? show-all-maps?)]
     [m/md-render entry put-fn editable? show-tags?]
     [i/image-view entry]
     [:hr]]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        store-snapshot @observed
        show-all-maps? (:show-all-maps local-snapshot)
        toggle-all-maps #(swap! local update-in [:show-all-maps] not)
        show-tags? (:show-hashtags local-snapshot)
        toggle-tags #(swap! local update-in [:show-hashtags] not)]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:span.fa.toggle-map.pull-right {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}]
      [:span.fa.fa-hashtag.toggle-map.pull-right {:class (when-not show-tags? "inactive") :on-click toggle-tags}]
      [:hr]
      (for [entry (:entries store-snapshot)]
        (let [ts (:timestamp entry)
              temp-entry (get-in store-snapshot [:temp-entries ts])
              show-map? (contains? (:show-maps-for store-snapshot) ts)
              editable? (contains? (:show-edit-for store-snapshot) ts)]
          ^{:key (:timestamp entry)}
          [journal-entry entry temp-entry put-fn show-map? editable? show-all-maps? show-tags?]))]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id        cmp-id
              :initial-state {:show-all-maps false
                              :show-hashtags true}
              :view-fn       journal-view
              :dom-id        "journal"}))

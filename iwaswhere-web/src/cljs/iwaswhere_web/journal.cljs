(ns iwaswhere-web.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.leaflet :as l]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.markdown :as m]
            [cljsjs.moment]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed local]}]
  (let [local-snapshot @local
        store-snapshot @observed
        show-all-maps? (:show-all-maps local-snapshot)
        show-hashtags? (:show-hashtags local-snapshot)]
    [:div:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:span.fa.toggle-map.pull-right
       {:class (if show-all-maps? "fa-map" "fa-map-o")
        :on-click #(swap! local update-in [:show-all-maps] not)}]
      [:span.fa.fa-hashtag.toggle-map.pull-right
       {:class (when-not show-hashtags? "inactive")
        :on-click #(swap! local update-in [:show-hashtags] not)}]
      [:hr]
      (let [entries (reverse (:entries store-snapshot))]
        (for [entry (take 50 (filter (h/entries-filter-fn (:new-entry store-snapshot)) entries))]
          (let [ts (:timestamp entry)
                map? (:latitude entry)
                show-map? (contains? (:show-maps-for local-snapshot) ts)]
            ^{:key ts}
            [:div.entry
             [:div.entry-header
              [:span.timestamp (.format (js/moment ts) "MMMM Do YYYY, h:mm:ss a")]
              (when map?
                [:span.fa.fa-map-o.toggle-map
                 {:on-click #(if show-map?
                              (swap! local update-in [:show-maps-for] disj ts)
                              (swap! local update-in [:show-maps-for] conj ts))}])]
             (when (and map? (or show-map? show-all-maps?))
               [l/leaflet-component {:id  (str "map" (:timestamp entry))
                                     :lat  (:latitude entry)
                                     :lon (:longitude entry)}])
             (m/markdown-render entry show-hashtags?)
             (when-let [img-file (:img-file entry)]
               [:a {:href   (str "/photos/" img-file) :target "_blank"}
                [:img {:src (str "/photos/" img-file)}]])
             [:hr]])))]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :initial-state {:show-maps-for #{}
                              :show-all-maps false
                              :show-hashtags true}
              :view-fn journal-view
              :dom-id  "journal"}))

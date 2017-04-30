(ns iwaswhere-web.ui.charts.location
  (:require [reagent.core :as rc]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.helpers :as h]
            [cljs.pprint :as pp]))

(defn location-chart [chart-h put-fn]
  (let [local (rc/atom {:last-fetched 0
                        :expand       false})
        stats (subscribe [:stats])
        emoji-flags (aget js/window "deps" "emojiFlags")
        last-update (subscribe [:last-update])]
    (fn [chart-h put-fn]
      (let [loc-stats (:locations @stats)
            expanded? (:expanded @local)
            per-country (->> (:locations @stats)
                             :days-per-country
                             (sort-by second)
                             reverse
                             (map-indexed (fn [idx v] [idx v])))]
        [:div.location-stats
         {:class (when expanded? "expanded")}
         [:div.content.white
          [:div.expand {:on-click #(swap! local update-in [:expanded] not)}
           [:span.fa {:class (if expanded? "fa-compress" "fa-expand")}]]
          [:table
           [:tbody
            (when expanded?
              [:tr
               [:th "Rank"]
               [:th "Flag"]
               [:th "Days"]
               [:th "Country"]])
            (for [[i [cc cnt]] (if expanded? per-country (take 5 per-country))]
              (let [country (js->clj (.countryCode emoji-flags cc))
                    flag (get country "emoji")
                    cname (get country "name")]
                ^{:key cc}
                [:tr
                 [:td.rank (inc i) "."]
                 [:td.flag flag]
                 [:td.country cname]
                 [:td.cnt cnt]]))]]
          (when expanded?
            [:div#plotly])]]))))

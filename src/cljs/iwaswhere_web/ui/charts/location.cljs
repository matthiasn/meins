(ns iwaswhere-web.ui.charts.location
  (:require [reagent.core :as rc]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.helpers :as h]
            [cljs.pprint :as pp]))

(defn wordcount-chart
  "Draws chart for wordcount per day. The size of the the bars scales
   automatically depending on the maximum count found in the data.
   On mouse-over on any of the bars, the date and the values for the date are
   shown in an info div next to the bars."
  [chart-h put-fn]
  (let [local (rc/atom {:last-fetched 0})
        stats (subscribe [:stats])
        emoji-flags (aget js/window "deps" "emojiFlags")
        last-update (subscribe [:last-update])]
    (fn [chart-h put-fn]
      (let [loc-stats (:locations @stats)
            days-per-country (sort-by second (:days-per-country loc-stats))]
        [:div.location-stats
         (for [[cc cnt] (reverse days-per-country)]
           (let [flag (get (js->clj (.countryCode emoji-flags cc)) "emoji")]
             ^{:key cc}
             [:div [:span.flag flag] [:span.cnt cnt "d"]]))]))))

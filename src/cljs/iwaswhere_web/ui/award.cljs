(ns iwaswhere-web.ui.award
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]))

(defn points-by-day-chart
  "Renders bars."
  [stats]
  (let [chart-h 12
        indexed (map-indexed (fn [idx [day v]] [idx [day v]]) stats)
        max-val (apply max (map (fn [[_ [d v]]] v) indexed))]
    [:svg
     [:g
      (for [[idx [day v]] indexed]
        (let [y-scale (/ chart-h (or max-val 1))
              h (if (pos? v) (* y-scale v) 5)]
          (when (pos? max-val)
            ^{:key (str day idx)}
            [:rect {:x      (* 10 idx)
                    :y      (- chart-h h)
                    :fill   "#7FE283"
                    :width  9
                    :height h}])))]]))

(defn award-points
  "Simple view for points awarded."
  [put-fn]
  (let [stats (subscribe [:stats])]
    (fn [put-fn]
      (let [award-points (:award-points @stats)
            by-day (sort-by first (:by-day award-points))]
        (prn by-day)
        [:div.award
         [:div.points (:total award-points)]
         "Award points"
         [points-by-day-chart by-day]]))))

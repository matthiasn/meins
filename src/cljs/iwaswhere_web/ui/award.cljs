(ns iwaswhere-web.ui.award
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]))

(defn points-by-day-chart
  "Renders bars."
  [stats]
  (let [chart-h 22
        indexed (map-indexed (fn [idx [day v]] [idx [day v]]) stats)
        daily-totals (map (fn [[_ [d v]]] (h/add (:task v) (:habit v))) indexed)
        max-val (apply max daily-totals)]
    [:svg
     [:g
      (for [[idx [day v]] indexed]
        (let [v (h/add (:task v) (:habit v))
              y-scale (/ chart-h (or max-val 1))
              h (if (pos? v) (* y-scale v) 0)]
          (when (pos? max-val)
            ^{:key (str day idx)}
            [:rect {:x      (* 10 idx)
                    :y      (- chart-h h)
                    :fill   "#7FE283"
                    :width  9
                    :height h}])))
      (for [[idx [day v]] indexed]
        (let [v (:task v)
              y-scale (/ chart-h (or max-val 1))
              h (if (pos? v) (* y-scale v) 0)]
          (when (pos? max-val)
            ^{:key (str day idx)}
            [:rect {:x      (* 10 idx)
                    :y      (- chart-h h)
                    :fill   "#42b8dd"
                    :width  9
                    :height h}])))]]))

(defn award-points
  "Simple view for points awarded."
  [put-fn]
  (let [stats (subscribe [:stats])]
    (fn [put-fn]
      (let [award-points (:award-points @stats)
            by-day (sort-by first (:by-day award-points))]
        [:div.award
         [:div.points [:span.fa.fa-diamond] (:total award-points)]
         [points-by-day-chart by-day]]))))

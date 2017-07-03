(ns iwaswhere-web.ui.charts.questionnaires
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]
            [goog.string :as gstring]
            [reagent.core :as r]
            [clojure.pprint :as pp]
            [clojure.string :as s]))

(defn points-by-day-chart
  "Renders bars."
  [stats]
  (let [chart-h 22
        daily-totals (map (fn [[d v]] (h/add (:task v) (:habit v))) stats)
        max-val (apply max daily-totals)
        indexed (map-indexed (fn [idx [day v]] [idx [day v]])
                             (take-last 14 stats))]
    [:svg
     [:g
      (for [[idx [day v]] indexed]
        (let [v (h/add (:task v) (:habit v))
              y-scale (/ chart-h (or max-val 1))
              h (if (pos? v) (* y-scale v) 0)]
          (when (pos? max-val)
            ^{:key (str day idx)}
            [:rect {:x      (* 8 idx)
                    :y      (- chart-h h)
                    :fill   "#7FE283"
                    :width  7
                    :height h}])))
      (for [[idx [day v]] indexed]
        (let [v (:task v)
              y-scale (/ chart-h (or max-val 1))
              h (if (pos? v) (* y-scale v) 0)]
          (when (pos? max-val)
            ^{:key (str day idx)}
            [:rect {:x      (* 8 idx)
                    :y      (- chart-h h)
                    :fill   "#42b8dd"
                    :width  7
                    :height h}])))]]))

(defn points-mapper
  [btm-y cnt]
  (let [cnt (if (pos-int? cnt) cnt 1)
        xw (/ 300 (dec cnt))]
    (fn [idx itm] (str (* xw idx) "," (- btm-y (* itm 2))))))

(defn chart-line [scores point-mapper color]
  (let [points (s/join " " (map-indexed point-mapper scores))]
    [:g
     [:g {:filter "url(#blur1)"}
      [:rect {:width  "100%"
              :height "100%"
              :style  {:fill   :none
                       :stroke :none}}]
      [:polyline {:points points
                  :style  {:stroke       color
                           :stroke-width 1
                           :fill         :none}}]]
     [:g
      [:polyline {:points points
                  :style  {:stroke       color
                           :stroke-width 1
                           :fill         :none}}]]]))

(defn line [y s w]
  [:line {:x1           1
          :x2           300
          :y1           y
          :y2           y
          :stroke-width w
          :stroke       s}])

(defn questionnaire-scores
  "Simple view for questionnaire scores."
  [put-fn]
  (let [stats (subscribe [:stats])
        last-update (subscribe [:last-update])
        local (r/atom {:last-fetched 0
                       :n            10})
        toggle-n (fn [_]
                   (let [n (if (= 10 (:n @local)) 50 10)]
                     (swap! local assoc-in [:n] n)))]
    (fn [put-fn]
      (let [scores (->> @stats
                        :questionnaires
                        :panas
                        (sort-by first)
                        (map second)
                        (filter seq)
                        (take-last (:n @local)))
            cnt (count scores)
            pos-scores (map :pos scores)
            neg-scores (map :neg scores)
            pos-mapper (points-mapper 100 cnt)
            neg-mapper (points-mapper 200 cnt)]
        [:div.questionnaires
         [:svg
          {:viewBox "0 0 300 210"
           :on-click toggle-n}
          [:filter#blur1
           [:feGaussianBlur {:stdDeviation 2}]]
          [:g
           [line 20 "#999" 1]
           [line 40 "#999" 1]
           [line 60 "#999" 1]
           [line 80 "#999" 1]
           [line 100 "#333" 2]
           [line 120 "#999" 1]
           [line 140 "#999" 1]
           [line 160 "#999" 1]
           [line 180 "#999" 1]
           [line 200 "#333" 2]]
          [chart-line neg-scores neg-mapper :red]
          [chart-line pos-scores pos-mapper :green]]]))))

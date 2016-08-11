(ns iwaswhere-web.ui.charts.activity
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]))

(defn weight-line
  "Draws line chart, for example for weight or LBM."
  [indexed local y-start y-end cls val-k]
  (let [chart-h (- y-end y-start)
        vals (filter second (map (fn [[k v]] [k (-> v :weight val-k)])
                                 indexed))
        max-val (or (apply max (map second vals)) 10)
        min-val (or (apply min (map second vals)) 1)
        y-scale (/ chart-h (- max-val min-val))
        mapper (fn [[idx v]]
                 (let [x (+ 5 (* 10 idx))
                       y (- (+ chart-h y-start) (* y-scale (- v min-val)))]
                   (str x "," y)))
        points (cc/line-points vals mapper)]
    [:g {:class cls}
     [:polyline {:points points}]
     (for [[idx v] (filter #(:weight (second %)) indexed)]
       (let [w (val-k (:weight v))
             mouse-enter-fn (cc/mouse-enter-fn local v)
             mouse-leave-fn (cc/mouse-leave-fn local v)
             cy (- (+ chart-h y-start) (* y-scale (- w min-val)))]
         ^{:key (str "weight" idx)}
         [:circle {:cx             (+ (* 10 idx) 5)
                   :cy             cy
                   :r              4
                   :on-mouse-enter mouse-enter-fn
                   :on-mouse-leave mouse-leave-fn}]))]))

(defn activity-bars
  "Renders bars for each day's activity."
  [indexed local y-start y-end]
  [:g
   (for [[idx v] indexed]
     (let [chart-h (- y-end y-start)
           max-val (apply max (map (fn [[_idx v]] (:total-exercise v)) indexed))
           y-scale (/ chart-h (or max-val 1))
           h (* y-scale (:total-exercise v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       ^{:key (str "actbar" idx)}
       [:rect {:x              (* 10 idx)
               :y              (- y-end h)
               :width          9
               :height         h
               :class          (cc/weekend-class "activity" v)
               :on-mouse-enter mouse-enter-fn
               :on-mouse-leave mouse-leave-fn}]))])

(defn activity-weight-chart
  "Draws chart for daily activities vs weight. Weight is a line chart with
   circles for each value, activites are represented as bars. On mouse-over
   on top of bars or circles, a small info div next to the hovered item is
   shown."
  [stats chart-h]
  (let [local (rc/atom {})]
    (fn [stats chart-h]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) stats)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [cc/chart-title "Weight/LBM/Activity"]
          [activity-bars indexed local 170 250]
          [weight-line indexed local 50 80 "lbm" :lbm]
          [weight-line indexed local 90 160 "weight" :value]]
         (when (:mouse-over @local)
           [:div.mouse-over-info
            {:style {:top  (- (:y (:mouse-pos @local)) 20)
                     :left (+ (:x (:mouse-pos @local)) 20)}}
            [:span (:date-string (:mouse-over @local))] [:br]
            [:span "Total min: " (:total-exercise (:mouse-over @local))] [:br]
            [:span "Weight: " (:value (:weight (:mouse-over @local)))] [:br]
            [:span "LBM: " (:lbm (:weight (:mouse-over @local)))]])]))))

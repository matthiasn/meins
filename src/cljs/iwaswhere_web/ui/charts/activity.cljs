(ns iwaswhere-web.ui.charts.activity
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]))

(defn draw-line
  "Draws line chart, for example for weight or LBM."
  [indexed local y-start y-end cls val-path show-key ctrl-x ctrl-y]
  (let [chart-h (- y-end y-start)
        vals (filter second (map (fn [[k v]] [k (get-in v val-path)])
                                 indexed))
        max-val (or (apply max (map second vals)) 10)
        min-val (or (apply min (map second vals)) 1)
        y-scale (/ chart-h (- max-val min-val))
        mapper (fn [[idx v]]
                 (let [x (+ 5 (* 10 idx))
                       y (- (+ chart-h y-start) (* y-scale (- v min-val)))]
                   (str x "," y)))
        points (cc/line-points vals mapper)
        toggle-show #(swap! local update-in [show-key] not)]
    [:g {:class cls}
     [:circle {:cx ctrl-x :cy ctrl-y :r 8 :on-click toggle-show}]
     (when (show-key @local)
       [:g
        [:polyline {:points points}]
        (for [[idx v] (filter #(get-in (second %) val-path) indexed)]
          (let [w (get-in v val-path)
                mouse-enter-fn (cc/mouse-enter-fn local v)
                mouse-leave-fn (cc/mouse-leave-fn local v)
                cy (- (+ chart-h y-start) (* y-scale (- w min-val)))]
            ^{:key (str val-path idx)}
            [:circle {:cx             (+ (* 10 idx) 5)
                      :cy             cy
                      :r              4
                      :on-mouse-enter mouse-enter-fn
                      :on-mouse-leave mouse-leave-fn}]))])]))

(defn activity-bars
  "Renders bars for each day's activity."
  [indexed local y-start y-end put-fn]
  [:g
   (for [[idx v] indexed]
     (let [chart-h (- y-end y-start)
           max-val (apply max (map (fn [[_idx v]] (:total-exercise v)) indexed))
           y-scale (/ chart-h (or max-val 1))
           h (* y-scale (:total-exercise v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       (when (pos? max-val)
         ^{:key (str "actbar" idx)}
         [:rect {:x              (* 10 idx)
                 :on-click       (cc/open-day-fn v put-fn)
                 :y              (- y-end h)
                 :width          9
                 :height         h
                 :class          (cc/weekend-class "activity" v)
                 :on-mouse-enter mouse-enter-fn
                 :on-mouse-leave mouse-leave-fn}])))])

(defn activity-weight-chart
  "Draws chart for daily activities vs weight. Weight is a line chart with
   circles for each value, activites are represented as bars. On mouse-over
   on top of bars or circles, a small info div next to the hovered item is
   shown."
  [stats chart-h put-fn]
  (let [local (rc/atom {:value true
                        :lbm   false
                        :girth true})]
    (fn [stats chart-h put-fn]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) stats)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [cc/chart-title "activity/weight/girth"]
          [cc/bg-bars indexed local chart-h :activity]
          [activity-bars indexed local 180 250 put-fn]
          [draw-line indexed local 50 130 "weight" [:weight :value] :value 20 20]
          [draw-line indexed local 50 130 "lbm" [:weight :lbm] :lbm 42 20]
          [draw-line indexed local 140 170 "girth" [:girth] :girth 64 20]]
         (when (:mouse-over @local)
           [:div.mouse-over-info (cc/info-div-pos @local)
            [:div (:date-string (:mouse-over @local))]
            (when-let [exercise (:total-exercise (:mouse-over @local))]
              [:div "Total min: " exercise])
            (when-let [weight (:value (:weight (:mouse-over @local)))]
              [:div "Weight: " weight])
            (when-let [lbm (:lbm (:weight (:mouse-over @local)))]
              [:div "LBM: " lbm])
            (when-let [girth (:girth (:mouse-over @local))]
              [:div "Girth: " (/ girth 10)])])]))))

(ns iwaswhere-web.ui.charts.daily-summaries
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]))

(defn bars
  "Renders bars for data set."
  [indexed k cls local y-start y-end put-fn]
  [:g
   (for [[idx v] indexed]
     (let [chart-h (- y-end y-start)
           max-val (apply max (map (fn [[_idx v]] (k v)) indexed))
           y-scale (/ chart-h (or max-val 1))
           h (* y-scale (k v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       (when (pos? max-val)
         ^{:key (str "bar" k idx)}
         [:rect {:x              (* 10 idx)
                 :on-click       (cc/open-day-fn v put-fn)
                 :y              (- y-end h)
                 :width          9
                 :height         h
                 :class          (cc/weekend-class cls v)
                 :on-mouse-enter mouse-enter-fn
                 :on-mouse-leave mouse-leave-fn}])))])

(defn stacked-bars-fn
  "Renders bars for data set."
  [indexed local put-fn]
  (fn [k-1 cls-1 k-2 cls-2 y-start y-end]
    (let [chart-h (- y-end y-start)
          max-val (apply max (map (fn [[_idx v]] (+ (k-1 v) (k-2 v))) indexed))
          y-scale (/ chart-h (or max-val 1))]
      [:g
       (for [[idx v] indexed]
         (let [h-1 (* y-scale (k-1 v))
               h-2 (* y-scale (k-2 v))
               mouse-enter-fn (cc/mouse-enter-fn local v)
               mouse-leave-fn (cc/mouse-leave-fn local v)
               common {:x              (* 10 idx)
                       :on-click       (cc/open-day-fn v put-fn)
                       :width          9
                       :on-mouse-enter mouse-enter-fn
                       :on-mouse-leave mouse-leave-fn}
               bar-1 {:y      (- y-end h-1)
                      :height h-1
                      :class  (cc/weekend-class cls-1 v)}
               bar-2 {:y      (- y-end h-2 h-1)
                      :height h-2
                      :class  (cc/weekend-class cls-2 v)}]
           (when (pos? max-val)
             ^{:key (str "bar" k-1 k-2 idx)}
             [:g
              [:rect (merge common bar-1)]
              [:rect (merge common bar-2)]])))])))

(defn daily-summaries-chart
  "Draws chart for daily activities vs weight. Weight is a line chart with
   circles for each value, activites are represented as bars. On mouse-over
   on top of bars or circles, a small info div next to the hovered item is
   shown."
  [stats chart-h put-fn]
  (let [local (rc/atom {})]
    (fn [stats chart-h put-fn]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) stats)
            stacked-bars (stacked-bars-fn indexed local put-fn)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [cc/chart-title "open tasks / backlog / completed"]
          [cc/bg-bars indexed local chart-h :daily-summaries]
          [stacked-bars :open-tasks-cnt "tasks" :backlog-cnt "backlog" 50 145]
          [stacked-bars :completed-cnt "done" :closed-cnt "closed" 155 250]]
         (when (:mouse-over @local)
           [:div.mouse-over-info (cc/info-div-pos @local)
            [:div (:date-string (:mouse-over @local))]
            [:div "Open Tasks: " (:open-tasks-cnt (:mouse-over @local))]
            [:div "Backlog: " (:backlog-cnt (:mouse-over @local))]
            [:div "Completed: " (:completed-cnt (:mouse-over @local))]
            [:div "Closed: " (:closed-cnt (:mouse-over @local))]])]))))

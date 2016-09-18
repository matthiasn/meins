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

(defn daily-summaries-chart
  "Draws chart for daily activities vs weight. Weight is a line chart with
   circles for each value, activites are represented as bars. On mouse-over
   on top of bars or circles, a small info div next to the hovered item is
   shown."
  [stats chart-h put-fn]
  (let [local (rc/atom {})]
    (fn [stats chart-h put-fn]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) stats)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [cc/chart-title "open tasks / backlog / completed"]
          [bars indexed :open-tasks-cnt "tasks" local 60 120 put-fn]
          [bars indexed :backlog-cnt "backlog" local 125 185 put-fn]
          [bars indexed :completed-cnt "done" local 190 250 put-fn]]
         (when (:mouse-over @local)
           [:div.mouse-over-info (cc/info-div-pos @local)
            [:div (:date-string (:mouse-over @local))]
            [:div "Open Tasks: " (:open-tasks-cnt (:mouse-over @local))]
            [:div "Backlog: " (:backlog-cnt (:mouse-over @local))]
            [:div "Completed: " (:completed-cnt (:mouse-over @local))]])]))))

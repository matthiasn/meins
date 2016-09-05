(ns iwaswhere-web.ui.charts.pomodoros
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]))

(defn bars
  [indexed local k chart-h y-scale]
  [:g
   (for [[idx v] indexed]
     (let [h (* y-scale (k v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       ^{:key (str "pbar" k idx)}
       [:rect {:class          (cc/weekend-class (name k) v)
               :x              (* 10 idx)
               :y              (- chart-h h)
               :width          9
               :height         h
               :on-mouse-enter mouse-enter-fn
               :on-mouse-leave mouse-leave-fn}]))])

(defn pomodoro-bar-chart
  [pomodoro-stats chart-h title y-scale]
  (let [local (rc/atom {})]
    (fn [pomodoro-stats chart-h title y-scale]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) pomodoro-stats)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [cc/chart-title title]
           [bars indexed local :total chart-h y-scale]
           [bars indexed local :completed chart-h y-scale]
           [cc/path "M 0 50 l 600 0 z"]
           [cc/path "M 0 100 l 600 0 z"]
           [cc/path "M 0 150 l 600 0 z"]
           [cc/path "M 0 200 l 600 0 z"]]]
         (when (:mouse-over @local)
           [:div.mouse-over-info (cc/info-div-pos @local)
            [:div (:date-string (:mouse-over @local))]
            [:div "Total: " (:total (:mouse-over @local))]
            [:div "Completed: " (:completed (:mouse-over @local))]
            [:div "Started: " (:started (:mouse-over @local))]])]))))

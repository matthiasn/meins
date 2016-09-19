(ns iwaswhere-web.ui.charts.wordcount
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]))

(defn wordcount-chart
  "Draws chart for wordcount per day. The size of the the bars scales
   automatically depending on the maximum count found in the data.
   On mouse-over on any of the bars, the date and the values for the date are
   shown in an info div next to the bars."
  [stats chart-h put-fn]
  (let [local (rc/atom {})]
    (fn [stats chart-h put-fn]
      (let [indexed (map-indexed (fn [idx [_k v]] [idx v]) stats)
            max-cnt (apply max (map (fn [[_idx v]] (:word-count v)) indexed))]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [cc/chart-title "Words per Day"]
           [cc/bg-bars indexed local chart-h :wordcount]
           (when (pos? max-cnt)
             (for [[idx v] indexed]
               (let [reserved 50
                     max-h (- chart-h reserved)
                     y-scale (/ max-h (or max-cnt 1))
                     h (* y-scale (:word-count v))
                     x (* 10 idx)
                     mouse-enter-fn (cc/mouse-enter-fn local v)
                     mouse-leave-fn (cc/mouse-leave-fn local v)]
                 ^{:key (str "tbar" (:date-string v) idx)}
                 [:g {:on-mouse-enter mouse-enter-fn
                      :on-mouse-leave mouse-leave-fn
                      :on-click       (cc/open-day-fn v put-fn)}
                  [:rect {:x      x
                          :y      (+ (- max-h h) reserved)
                          :width  9
                          :height h
                          :class  (cc/weekend-class "done" v)}]])))]]
         (when (:mouse-over @local)
           [:div.mouse-over-info (cc/info-div-pos @local)
            [:div (:date-string (:mouse-over @local))]
            [:div "Words: " (:word-count (:mouse-over @local))]])]))))

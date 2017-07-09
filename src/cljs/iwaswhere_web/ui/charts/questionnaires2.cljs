(ns iwaswhere-web.ui.charts.questionnaires2
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]
            [goog.string :as gstring]
            [reagent.core :as r]
            [clojure.pprint :as pp]
            [clojure.string :as s]
            [matthiasn.systems-toolbox.component :as st]
            [iwaswhere-web.ui.charts.common :as cc]))

(def month-day "DD.MM.")
(def ymd "YYYY-MM-DD")
(def weekday "ddd")
(defn df [ts format] (.format (js/moment ts) format))

(defn tick
  "Renders individual timeline tick."
  [pos color w h base-y]
  (let [half-h (/ h 2)]
    [:line
     {:x1           pos
      :y1           (- base-y half-h)
      :x2           pos
      :y2           (+ base-y half-h)
      :stroke       color
      :stroke-width w}]))

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
  [btm-y k start end]
  (let [x-offset 200
        span (- end start)]
    (fn [idx itm]
      (let [ts (:timestamp itm)
            from-beginning (- ts start)
            x (* 900 (/ from-beginning span))]
        (str (+ x-offset x) "," (- btm-y (* (k itm) 2)))))))

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
                           :stroke-width 2
                           :fill         :none}}]]
     [:g
      [:polyline {:points points
                  :style  {:stroke       color
                           :stroke-width 1
                           :fill         :none}}]]]))

(defn line [y s w]
  [:line {:x1           200
          :x2           1100
          :y1           y
          :y2           y
          :stroke-width w
          :stroke       s}])

(defn barchart-row
  [days span start stats tag k y scale cls]
  [:g
   [:text {:x           180
           :y           (- y 12)
           :font-size   12
           :fill        "#777"
           :font-weight :bold
           :text-anchor "end"}
    tag]
   (for [n (range (inc days))]
     (let [d (* 24 60 60 1000)
           offset (* n d)
           scaled (* 900 (/ offset span))
           x (+ 205 scaled)
           ts (+ start offset)
           ymd (df ts ymd)
           v (get-in stats [ymd tag k] 0)
           h (* v scale)
           weekday (df ts weekday)
           weekend? (get #{"Sat" "Sun"} weekday)]
       ^{:key n}
       [:rect {:x      x
               :y      (- y h)
               :width  20
               :height h
               :class  (cc/weekend-class cls ymd)}]))])

(defn questionnaire-scores
  "Simple view for questionnaire scores."
  [put-fn]
  (let [stats (subscribe [:stats])
        custom-field-stats (subscribe [:custom-field-stats])
        last-update (subscribe [:last-update])
        local (r/atom {:last-fetched 0 :n 30})
        toggle (fn [_] (swap! local assoc-in [:n] (if (= 7 (:n @local)) 30 7)))]
    (fn [put-fn]
      (prn :keep-updated)
      (h/keep-updated :stats/custom-fields 31 local @last-update put-fn)
      (let [scores2 (->> @stats
                         :questionnaires
                         :panas
                         (sort-by first)
                         (filter #(seq (second %)))
                         (map (fn [[ts m]] (assoc-in m [:timestamp] ts))))
            days (:n @local)
            now (st/now)
            d (* 24 60 60 1000)
            within-day (mod now d)
            start (- now within-day (* days d))
            end (+ (- now within-day) d)
            span (- end start)
            pos-scores (filter :pos scores2)
            neg-scores (filter :neg scores2)
            pos-mapper (points-mapper 200 :pos start end)
            neg-mapper (points-mapper 282 :neg start end)
            custom-field-stats @custom-field-stats]
        [:div.questionnaires
         [:svg {:viewBox  "0 0 1200 500"
                :style    {:background :white}
                :on-click toggle}
          [:filter#blur1
           [:feGaussianBlur {:stdDeviation 3}]]
          [:g
           (for [n (range (+ 2 days))]
             (let [offset (* n d)
                   scaled (* 900 (/ offset span))
                   x (+ 200 scaled)]
               ^{:key n}
               [tick x "#CCC" 1 312 240]))
           [line 100 "#333" 2]
           [line 120 "#888" 1]
           [line 140 "#888" 1]
           [line 160 "#888" 1]
           [line 181 "#333" 2]
           [line 202 "#888" 1]
           [line 222 "#888" 1]
           [line 242 "#888" 1]
           [line 263 "#333" 2]
           [line 296 "#333" 2]
           [line 329 "#333" 2]
           [line 362 "#333" 2]
           [line 395 "#333" 2]]
          [chart-line neg-scores neg-mapper :red]
          [chart-line pos-scores pos-mapper :green]
          [:rect {:fill :white :x 0 :y 0 :height 600 :width 200}]
          [:text {:x           180
                  :y           145
                  :font-size   12
                  :fill        "#777"
                  :font-weight :bold
                  :text-anchor "end"}
           "Positive Affect Score"]
          [:text {:x           180
                  :y           226
                  :font-size   12
                  :fill        "#777"
                  :font-weight :bold
                  :text-anchor "end"}
           "Negative Affect Score"]
          (for [n (range (inc days))]
            (let [offset (* (+ n 0.5) d)
                  scaled (* 900 (/ offset span))
                  x (+ 200 scaled)
                  ts (+ start offset)
                  weekday (df ts weekday)
                  weekend? (get #{"Sat" "Sun"} weekday)]
              ^{:key n}
              [:text {:x           x
                      :y           90
                      :font-size   6
                      :fill        (if weekend? :red :black)
                      :font-weight :bold
                      :text-anchor "middle"}
               (df ts month-day)]))
          [barchart-row days span start custom-field-stats "#sit-ups" :cnt 295 0.3 "done"]
          [barchart-row days span start custom-field-stats "#coffee" :cnt 328 0.04 "failed"]
          [barchart-row days span start custom-field-stats "#steps" :cnt 361 0.0015 "done"]
          [barchart-row days span start custom-field-stats "#running" :distance 394 0.6 "done"]]]))))

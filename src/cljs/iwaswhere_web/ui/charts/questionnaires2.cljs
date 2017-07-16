(ns iwaswhere-web.ui.charts.questionnaires2
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]
            [reagent.ratom :refer-macros [reaction]]
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
  [:line
   {:x1           pos
    :y1           base-y
    :x2           pos
    :y2           (+ base-y h)
    :stroke       color
    :stroke-width w}])

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

(defn rect
  [v x y h cls n]
  (let [local (r/atom {})
        click (fn [_] (swap! local update-in [:show-label] not))]
    (fn [v x y h cls n]
      [:g
       [:rect {:on-click click
               :x        x
               :y        (- y h)
               :width    23
               :height   h
               :class    (cc/weekend-class cls ymd)}]
       (when (:show-label @local)
         [:text {:x           (+ x 11)
                 :y           (- y 5)
                 :font-size   8
                 :fill        "#777"
                 :text-anchor "middle"}
          v])])))

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
           scaled (* n 29)
           x (+ 203 scaled)
           ts (+ start offset)
           ymd (df ts ymd)
           v (get-in stats [ymd tag k] 0)
           h (* v scale)
           weekday (df ts weekday)
           weekend? (get #{"Sat" "Sun"} weekday)
           display-v (if (= :duration k)
                       (h/m-to-hh-mm v)
                       v)]
       ^{:key n}
       [rect display-v x y h cls n]))])


(defn points-by-day-chart
  "Renders bars."
  [y]
  (let [stats (subscribe [:stats])]
    (fn [y]
      (let [award-points (:award-points @stats)
            by-day (sort-by first (:by-day award-points))
            chart-h 30
            daily-totals (map (fn [[d v]] (h/add (:task v) (:habit v))) by-day)
            max-val (apply max daily-totals)
            indexed (map-indexed (fn [idx [day v]] [idx [day v]])
                                 (take-last 30 by-day))]
        [:g
         (for [[idx [day v]] indexed]
           (let [v (h/add (:task v) (:habit v))
                 y-scale (/ chart-h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      (+ 203 (* 29 idx))
                       :y      (- y h)
                       :fill   "#7FE283"
                       :width  23
                       :height h}])))
         (for [[idx [day v]] indexed]
           (let [v (:task v)
                 y-scale (/ chart-h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      (+ 203 (* 29 idx))
                       :y      (- y h)
                       :fill   "#42b8dd"
                       :width  23
                       :height h}])))]))))

(defn scores-fn
  [stats k]
  (->> stats
       :questionnaires
       k
       (sort-by first)
       (filter #(seq (second %)))
       (map (fn [[ts m]] (assoc-in m [:timestamp] ts)))))

(defn scores-chart
  [{:keys [y k w h score-k start end mn mx color x-offset label]}]
  (let [stats (subscribe [:stats])
        scores (reaction (filter score-k (scores-fn @stats k)))
        span (- end start)
        rng (- mx mn)
        scale (/ h rng)
        btm-y (+ y h)
        mapper (fn [idx itm]
                 (let [ts (:timestamp itm)
                       from-beginning (- ts start)
                       x (* w (/ from-beginning span))]
                   (str (+ x-offset x) ","
                        (- btm-y (* (- (score-k itm) mn) scale)))))
        lines (filter #(zero? (mod % 10)) (range 1 rng))]
    (fn scores-chart-render [{:keys [y k score-k start end mn mx color]}]
      [:g
       (for [n lines]
         ^{:key (str k score-k n)}
         [line (- btm-y (* n scale)) "#888" 1])
       [chart-line @scores mapper color]
       [line y "#000" 2]
       [line (+ y h) "#000" 2]
       [:rect {:fill :white :x 0 :y y :height (+ h 5) :width 200}]
       [:text {:x           180
               :y           (+ y (+ 5 (/ h 2)))
               :font-size   12
               :fill        "#777"
               :font-weight :bold
               :text-anchor "end"}
        label]])))

(def charts-cfg
  [{:h       60
    :k       :panas
    :score-k :pos
    :label   "Positive Affect Score"
    :mn      10
    :mx      50
    :color   :green}
   {:h       60
    :k       :panas
    :score-k :neg
    :label   "Negative Affect Score"
    :mn      10
    :mx      50
    :color   :red}
   {:h       40
    :k       :cfq11
    :score-k :total
    :label   "CFQ11"
    :mn      0
    :mx      33
    :color   :blue}])

(defn charts-y-pos
  [cfg]
  (reduce
    (fn [acc m]
      (let [{:keys [last-y last-h]} acc
            cfg (assoc-in m [:y] (+ last-y last-h))]
        {:last-y (:y cfg)
         :last-h (:h cfg)
         :charts (conj (:charts acc) cfg)}))
    {:last-y 50
     :last-h 0}
    cfg))

(defn dashboard
  "Simple view for questionnaire scores."
  [put-fn]
  (let [custom-field-stats (subscribe [:custom-field-stats])
        last-update (subscribe [:last-update])
        local (r/atom {:n 30})
        toggle (fn [_] (swap! local assoc-in [:n] (if (= 7 (:n @local)) 30 7)))]
    (fn dashboard-render [put-fn]
      (h/keep-updated :stats/custom-fields 31 local @last-update put-fn)
      (let [days (:n @local)
            now (st/now)
            d (* 24 60 60 1000)
            within-day (mod now d)
            start (- now within-day (* days d))
            end (+ (- now within-day) d)
            span (- end start)
            custom-field-stats @custom-field-stats
            common {:start start :end end :w 900 :x-offset 200}
            positioned-charts (:charts (charts-y-pos charts-cfg))]
        [:div.questionnaires
         [:svg {:viewBox "0 0 1200 800"
                :style   {:background :white}
                ;:on-click toggle
                }
          [:filter#blur1
           [:feGaussianBlur {:stdDeviation 3}]]
          [:g
           (for [n (range (+ 2 days))]
             (let [offset (* n d)
                   scaled (* 900 (/ offset span))
                   x (+ 200 scaled)]
               ^{:key n}
               [tick x "#CCC" 1 430 30]))
           [line 296 "#333" 2]
           [line 329 "#333" 2]
           [line 362 "#333" 2]
           [line 395 "#333" 2]
           [line 428 "#333" 2]
           [line 461 "#333" 2]]
          (for [chart-cfg positioned-charts]
            ^{:key (:label chart-cfg)}
            [scores-chart (merge common chart-cfg)])
          (for [n (range (inc days))]
            (let [offset (* (+ n 0.5) d)
                  scaled (* 900 (/ offset span))
                  x (+ 200 scaled)
                  ts (+ start offset)
                  weekday (df ts weekday)
                  weekend? (get #{"Sat" "Sun"} weekday)]
              ^{:key n}
              [:text {:x           x
                      :y           40
                      :font-size   6
                      :fill        (if weekend? :red :black)
                      :font-weight :bold
                      :text-anchor "middle"}
               (df ts month-day)]))
          [barchart-row days span start custom-field-stats "#sit-ups" :cnt 295 0.3 "done"]
          [barchart-row days span start custom-field-stats "#coffee" :cnt 328 0.04 "failed"]
          [barchart-row days span start custom-field-stats "#steps" :cnt 361 0.0015 "done"]
          [barchart-row days span start custom-field-stats "#sleep" :duration 394 0.05 "done"]
          [barchart-row days span start custom-field-stats "#running" :distance 427 2 "done"]
          [points-by-day-chart 460]]]))))

(ns iwaswhere-web.ui.charts.questionnaires
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
  [pos color w y1 y2]
  [:line
   {:x1           pos
    :y1           y1
    :x2           pos
    :y2           y2
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

(defn indexed-days
  [stats tag k start days]
  (let [d (* 24 60 60 1000)
        rng (range (inc days))
        indexed (map-indexed (fn [n v]
                               (let [offset (* n d)
                                     ts (+ start offset)
                                     ymd (df ts ymd)
                                     v (get-in stats [ymd tag k] 0)
                                     weekday (df ts weekday)]
                                 [n {:ymd     ymd
                                     :v       v
                                     :weekday weekday}]))
                             rng)]
    indexed))

(defn barchart-row
  [{:keys [days span start stats tag k h y cls]}]
  (let [btm-y (+ y h)
        indexed (indexed-days stats tag k start days)
        mx (apply max (map #(:v (second %)) indexed))
        scale (if (pos? mx) (/ (- h 3) mx) 1)]
    [:g
     [:text {:x           180
             :y           (+ y (+ 5 (/ h 2)))
             :font-size   12
             :fill        "#777"
             :font-weight :bold
             :text-anchor "end"}
      tag]
     (for [[n {:keys [ymd v weekday]}] indexed]
       (let [d (* 24 60 60 1000)
             offset (* n d)
             scaled (* 900 (/ offset span))
             scaled (* n 29)
             x (+ 203 scaled)
             h (* v scale)
             weekend? (get #{"Sat" "Sun"} weekday)
             display-v (if (= :duration k)
                         (h/m-to-hh-mm v)
                         v)]
         ^{:key (str tag n)}
         [rect display-v x btm-y h cls n]))
     [line (+ y h) "#000" 2]]))


(defn points-by-day-chart
  "Renders bars."
  [{:keys [y h label]}]
  (let [stats (subscribe [:stats])
        btm-y (+ y h)]
    (fn points-by-day-render [{:keys [y h label]}]
      (let [award-points (:award-points @stats)
            by-day (sort-by first (:by-day award-points))
            daily-totals (map (fn [[d v]] (h/add (:task v) (:habit v))) by-day)
            max-val (apply max daily-totals)
            indexed (map-indexed (fn [idx [day v]] [idx [day v]])
                                 (take-last 31 by-day))]
        [:g
         (for [[idx [day v]] indexed]
           (let [v (h/add (:task v) (:habit v))
                 y-scale (/ h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      (+ 203 (* 29 idx))
                       :y      (- btm-y h)
                       :fill   "#7FE283"
                       :width  23
                       :height h}])))
         (for [[idx [day v]] indexed]
           (let [v (:task v)
                 y-scale (/ h (or max-val 1))
                 h (if (pos? v) (* y-scale v) 0)]
             (when (pos? max-val)
               ^{:key (str day idx)}
               [:rect {:x      (+ 203 (* 29 idx))
                       :y      (- btm-y h)
                       :fill   "#42b8dd"
                       :width  23
                       :height h}])))
         [line (+ y h) "#000" 2]
         [:text {:x           180
                 :y           (+ y (+ 5 (/ h 2)))
                 :font-size   12
                 :fill        "#777"
                 :font-weight :bold
                 :text-anchor "end"}
          label]]))))

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
        options (subscribe [:options])
        questionnaires (reaction (:questionnaires @options))
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
            common {:start start :end end :w 900 :x-offset 200
                    :span  span :days days :stats custom-field-stats}
            charts-cfg (get-in @questionnaires [:dashboards :dashboard-1])
            positioned-charts (charts-y-pos charts-cfg)
            end-y (+ (:last-y positioned-charts) (:last-h positioned-charts))]
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
               [tick x "#CCC" 1 30 end-y]))]
          (for [chart-cfg (:charts positioned-charts)]
            (let [chart-fn (case (:type chart-cfg)
                             :scores-chart scores-chart
                             :barchart-row barchart-row
                             :points-by-day points-by-day-chart)]
              ^{:key (str (:label chart-cfg) (:tag chart-cfg))}
              [chart-fn (merge common chart-cfg)]))
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
               (df ts month-day)]))]]))))

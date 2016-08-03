(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [cljsjs.moment]
            [iwaswhere-web.ui.pomodoro :as p]))

(def ymd-format "YYYY-MM-DD")

(defn get-pomo-stats
  "Retrieves pomodoro stats for the last n days."
  [put-fn n]
  (doseq [ds (map #(.format (.subtract (js/moment.) % "d") ymd-format)
                  (range n))]
    (put-fn [:stats/pomo-day-get {:date-string ds}])))

(defn line-chart
  [points stroke]
  [:svg
   {:viewBox "0 0 600 200"}
   [:polyline
    {:fill         :none
     :stroke       stroke
     :stroke-width 1
     :points       points}]])

(defn line-points
  [indexed h]
  (let [point-strings (map (fn [[idx v]]
                             (str (* 10 idx) "," (- h (* 10 v)))) indexed)]
    (apply str (interpose " " point-strings))))

(defn bar-chart
  [indexed fill-weekday fill-weekend chart-h]
  [:svg
   {:viewBox "0 0 600 200"}
   [:g
    (for [[idx v] indexed]
      (let [fill fill-weekday
            h (* 10 v)
            x (* 10 idx)
            y (- chart-h h)]
        ^{:key (str "pbar" idx)}
        [:rect {:x x :y y :fill fill :width 9 :height h}]))]])

(defn path
  "Renders path with the given path description attribute."
  [d]
  [:path {:stroke "rgba(200,200,200,0.5)":stroke-width 1 :d d}])

(defn pomodoro-chart
  [pomodoro-stats stats-key fill-weekday fill-weekend chart-h title]
  (let [indexed (map-indexed (fn [idx [k v]] [idx v]) pomodoro-stats)]
    [:svg
     {:viewBox (str "0 0 600 " chart-h)}
     [:g
      [:text {:x           300
              :y           32
              :stroke      "none"
              :fill        "#AAA"
              :text-anchor :middle
              :style       {:font-weight :bold
                            :font-size   24}}
       title]
      (for [[idx v] indexed]
        (let [day-of-week (.weekday (js/moment. (:date-string v)))
              fill (if (and (pos? day-of-week) (< day-of-week 6))
                     fill-weekday
                     fill-weekend)
              h (* 10 (stats-key v))
              x (* 10 idx)
              y (- chart-h h)]
          ^{:key (str "pbar" stats-key idx)}
          [:rect {:x x :y y :fill fill :width 9 :height h}]))
      [path "M 0 50 l 600 0 z"]
      [path "M 0 100 l 600 0 z"]
      [path "M 0 150 l 600 0 z"]
      [path "M 0 200 l 600 0 z"]]]))

(defn stats-view
  "Renders stats component."
  [{:keys [observed local put-fn]}]
  (let [store-snapshot @observed
        pomodoro-stats (:pomodoro-stats store-snapshot)
        cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries store-snapshot))
        totals (map-indexed (fn [idx [k v]] [idx (:total v)]) pomodoro-stats)]
    [:div.stats
     ;[line-chart (line-points totals 200) "#0074d9"]
     ;[bar-chart totals "steelblue" "#cc5500" 200]
     [pomodoro-chart pomodoro-stats :total "steelblue" "#cc5500" 250
      "Pomodoros total"]
     [pomodoro-chart pomodoro-stats :completed "steelblue" "#cc5500" 250
      "Pomodoros completed"]
     (when-let [stats (:stats store-snapshot)]
       [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, "
        (:edge-count stats) " edges, " (count (:hashtags cfg)) " hashtags, "
        (count (:mentions cfg)) " people"])
     (when-let [ms (get-in store-snapshot [:timing :query])]
       [:div.stats
        (str "Query with " (count entries)
             " results completed in " ms ", RTT "
             (get-in store-snapshot [:timing :rtt]) " ms")])]))

(defn init-fn
  ""
  [{:keys [local observed put-fn]}]
  (let []))

(defn update-stats
  [{:keys [put-fn]}]
  (get-pomo-stats put-fn 60))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id      cmp-id
              :init-fn     init-fn
              :handler-map {:state/new update-stats}
              :view-fn     stats-view
              :dom-id      "stats"}))

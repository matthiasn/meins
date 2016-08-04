(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [cljsjs.moment]
            [cljs.pprint :as pp]
            [reagent.core :as rc]))

(def ymd-format "YYYY-MM-DD")
(defn n-days-go [n] (.subtract (js/moment.) n "d"))
(defn n-days-go-fmt [n] (.format (n-days-go n) ymd-format))

(defn get-stats
  "Retrieves pomodoro stats for the last n days."
  [stats-key put-fn n]
  (doseq [ds (map n-days-go-fmt (range n))]
    (put-fn [stats-key {:date-string ds}])))

(defn line-chart
  [points stroke]
  [:svg
   {:viewBox "0 0 600 200"}
   [:polyline
    {:fill :none :stroke stroke :stroke-width 1 :points points}]])

(defn line-points
  [indexed h]
  (let [point-strings (map (fn [[idx v]]
                             (str (* 10 idx) "," (- h (* 10 v)))) indexed)]
    (apply str (interpose " " point-strings))))

(defn path
  "Renders path with the given path description attribute."
  [d]
  [:path {:stroke "rgba(200,200,200,0.5)" :stroke-width 1 :d d}])

(defn bar-chart
  [pomodoro-stats stats-key fill-weekday fill-weekend chart-h title y-scale]
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
              h (* y-scale (stats-key v))
              x (* 10 idx)
              y (- chart-h h)]
          ^{:key (str "pbar" stats-key idx)}
          [:rect {:x x :y y :fill fill :width 9 :height h}]))
      [path "M 0 50 l 600 0 z"]
      [path "M 0 100 l 600 0 z"]
      [path "M 0 150 l 600 0 z"]
      [path "M 0 200 l 600 0 z"]]]))

(defn pomodoro-bar-chart
  [pomodoro-stats fill-weekday fill-weekend chart-h title y-scale]
  (let [local (rc/atom {})]
    (fn [pomodoro-stats fill-weekday fill-weekend chart-h title y-scale]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) pomodoro-stats)]
        [:div
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
                   h (* y-scale (:total v))
                   x (* 10 idx)
                   y (- chart-h h)
                   mouse-enter-fn (fn [ev]
                                    (reset! local
                                            {:mouse-over v
                                             :mouse-pos {:x (.-pageX ev)
                                                         :y (.-pageY ev)}}))
                   mouse-leave-fn (fn [_ev]
                                    (when (= v (:mouse-over @local))
                                      (reset! local {})))]
               ^{:key (str "pbar" (:total v) idx)}
               [:rect {:x              x
                       :y              y
                       :fill           fill
                       :width          9
                       :height         h
                       :on-mouse-enter mouse-enter-fn
                       :on-mouse-leave mouse-leave-fn}]))
           [path "M 0 50 l 600 0 z"]
           [path "M 0 100 l 600 0 z"]
           [path "M 0 150 l 600 0 z"]
           [path "M 0 200 l 600 0 z"]]]
         (when (:mouse-over @local)
           [:div.mouse-over-info
            {:style {:top  (- (:y (:mouse-pos @local)) 20)
                     :left (+ (:x (:mouse-pos @local)) 20)}}
            [:span (:date-string (:mouse-over @local))] [:br]
            [:span "Total: " (:total (:mouse-over @local))] [:br]
            [:span "Completed: " (:completed (:mouse-over @local))] [:br]
            [:span "Started: " (:started (:mouse-over @local))] [:br]])]))))

(defn stats-view
  "Renders stats component."
  [{:keys [observed]}]
  (let [store-snapshot @observed
        pomodoro-stats (:pomodoro-stats store-snapshot)
        activity-stats (:activity-stats store-snapshot)
        cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries store-snapshot))]
    [:div.stats
     [pomodoro-bar-chart pomodoro-stats "steelblue" "#cc5500" 250
      "Pomodoros total" 10]
     [bar-chart pomodoro-stats :completed "steelblue" "#cc5500" 250
      "Pomodoros completed" 10]
     [bar-chart activity-stats :total-exercise "steelblue" "#cc5500" 250
      "Activity minutes" 1]
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
  (get-stats :stats/pomo-day-get put-fn 60)
  (get-stats :stats/activity-day-get put-fn 60))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id      cmp-id
              :init-fn     init-fn
              :handler-map {:state/new update-stats}
              :view-fn     stats-view
              :dom-id      "stats"}))

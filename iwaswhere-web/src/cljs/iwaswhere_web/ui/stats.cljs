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
  (doseq [ds (map n-days-go-fmt (reverse (range n)))]
    (put-fn [stats-key {:date-string ds}])))

(defn line-points
  [indexed mapper]
  (let [point-strings (map mapper indexed)]
    (apply str (interpose " " point-strings))))

(defn path
  "Renders path with the given path description attribute."
  [d]
  [:path {:stroke "rgba(200,200,200,0.5)" :stroke-width 1 :d d}])

(defn weekend?
  [date-string]
  (let [day-of-week (.weekday (js/moment. date-string))]
    (not (and (pos? day-of-week) (< day-of-week 6)))))

(defn weekend-class
  [cls v]
  (str cls (when (weekend? (:date-string v)) "-weekend")))

(defn chart-title
  [title]
  [:text {:x           300
          :y           32
          :stroke      "none"
          :fill        "#AAA"
          :text-anchor :middle
          :style       {:font-weight :bold
                        :font-size   24}}
   title])

(defn mouse-enter-fn
  "Creates event handler for mouse-enter events on elements in a chart.
   Takes a local atom and the value associated with the chart element.
   Returns function which detects the mouse position from the event and
   replaces :mouse-over key in local atom with v and :mouse-pos with the
   mouse position in the event"
  [local v]
  (fn [ev]
    (let [mouse-pos {:x (.-pageX ev) :y (.-pageY ev)}
          update-fn (fn [state v]
                      (-> state
                          (assoc-in [:mouse-over] v)
                          (assoc-in [:mouse-pos] mouse-pos)))]
      (swap! local update-fn v))))

(defn mouse-leave-fn
  [local v]
  (fn [_ev]
    (when (= v (:mouse-over @local)) (reset! local {}))))

(defn activity-weight-chart
  "Draws chart for daily activities vs weight. Weight is a line chart with
   circles for each value, activites are represented as bars. On mouse-over
   on top of bars or circles, a small info div next to the hovered item is
   shown."
  [stats chart-h]
  (let [local (rc/atom {})]
    (fn [stats chart-h]
      (let [headline-reserved 50
            chart-h-half (/ (- chart-h headline-reserved) 2)
            indexed (map-indexed (fn [idx [k v]] [idx v]) stats)
            weights (map (fn [[k v]] [k (-> v :weight :value)]) indexed)
            weights (filter second weights)
            max-weight (or (apply max (map second weights)) 10)
            min-weight (or (apply min (map second weights)) 1)
            y-scale-weight (/ chart-h-half (- max-weight min-weight))
            mapper (fn [[idx v]]
                     (str (+ 5 (* 10 idx)) ","
                          (- (+ chart-h-half headline-reserved)
                             (* y-scale-weight (- v min-weight)))))
            points (line-points weights mapper)
            max-val (apply max (map (fn [[_idx v]] (:total-exercise v)) indexed))
            y-scale (/ chart-h-half (or max-val 1))]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [chart-title "Activity/Weight"]
           (for [[idx v] indexed]
             (let [h (* y-scale (:total-exercise v))
                   mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)]
               ^{:key (str "actbar" idx)}
               [:rect {:x              (* 10 idx)
                       :y              (- chart-h h)
                       :width          9
                       :height         h
                       :class          (weekend-class "activity" v)
                       :on-mouse-enter mouse-enter-fn
                       :on-mouse-leave mouse-leave-fn}]))]
          [:g
           [:polyline
            {:fill :none :stroke :steelblue :stroke-width 2 :points points}]
           (for [[idx v] (filter #(:weight (second %)) indexed)]
             (let [w (:value (:weight v))
                   mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)
                   cy (- (+ chart-h-half headline-reserved)
                         (* y-scale-weight (- w min-weight)))]
               ^{:key (str "weight" idx)}
               [:circle {:cx             (+ (* 10 idx) 5)
                         :cy             cy
                         :r              4
                         :stroke         :steelblue
                         :stroke-width   1
                         :fill           :lightblue
                         :on-mouse-enter mouse-enter-fn
                         :on-mouse-leave mouse-leave-fn}]))]]
         (when (:mouse-over @local)
           [:div.mouse-over-info
            {:style {:top  (- (:y (:mouse-pos @local)) 20)
                     :left (+ (:x (:mouse-pos @local)) 20)}}
            [:span (:date-string (:mouse-over @local))] [:br]
            [:span "Total min: " (:total-exercise (:mouse-over @local))] [:br]
            [:span "Weight: " (:value (:weight (:mouse-over @local)))]])]))))

(defn tasks-chart
  "Draws chart for opened and closed tasks, where the bars for the counts of
   newly opened tasks are drawn above a horizontal line and those for closed
   tasks below this line. The size of the the bars scales automatically
   depending on the maximum count found in the data.
   On mouse-over on any of the bars, the date and the values for the date are
   shown in an info div next to the bars."
  [task-stats chart-h]
  (let [local (rc/atom {})]
    (fn [task-stats chart-h]
      (let [indexed (map-indexed (fn [idx [_k v]] [idx v]) task-stats)
            max-cnt (apply max (map (fn [[_idx v]]
                                      (max (:tasks-cnt v) (:done-cnt v)))
                                    indexed))]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [chart-title "Tasks opened/closed"]
           (for [[idx v] indexed]
             (let [headline-reserved 50
                   chart-h-half (/ (- chart-h headline-reserved) 2)
                   y-scale (/ chart-h-half (or max-cnt 1))
                   h-tasks (* y-scale (:tasks-cnt v))
                   h-done (* y-scale (:done-cnt v))
                   x (* 10 idx)
                   mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)]
               ^{:key (str "tbar" (:date-string v) idx)}
               [:g {:on-mouse-enter mouse-enter-fn
                    :on-mouse-leave mouse-leave-fn}
                [:rect {:x      x
                        :y      (+ (- chart-h-half h-tasks) headline-reserved)
                        :width  9
                        :height h-tasks
                        :class  (weekend-class "tasks" v)}]
                [:rect {:x      x
                        :y      (+ chart-h-half headline-reserved)
                        :width  9
                        :height h-done
                        :class  (weekend-class "done" v)}]]))]]
         (when (:mouse-over @local)
           [:div.mouse-over-info
            {:style {:top  (- (:y (:mouse-pos @local)) 20)
                     :left (+ (:x (:mouse-pos @local)) 20)}}
            [:span (:date-string (:mouse-over @local))] [:br]
            [:span "Done: " (:done-cnt (:mouse-over @local))] [:br]
            [:span "Created: " (:tasks-cnt (:mouse-over @local))]
            [:br]])]))))

(defn bars
  [indexed local k chart-h y-scale]
  [:g
   (for [[idx v] indexed]
     (let [h (* y-scale (k v))
           mouse-enter-fn (mouse-enter-fn local v)
           mouse-leave-fn (mouse-leave-fn local v)]
       ^{:key (str "pbar" k idx)}
       [:rect {:class          (weekend-class (name k) v)
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
           [chart-title title]
           [bars indexed local :total chart-h y-scale]
           [bars indexed local :completed chart-h y-scale]
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
        task-stats (:task-stats store-snapshot)
        cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries store-snapshot))]
    [:div.stats
     [:div.charts
      [pomodoro-bar-chart pomodoro-stats 250 "Pomodoros" 10]
      [activity-weight-chart activity-stats 250]
      [tasks-chart task-stats 250]]
     (when-let [stats (:stats store-snapshot)]
       [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, "
        (:edge-count stats) " edges, " (count (:hashtags cfg)) " hashtags, "
        (count (:mentions cfg)) " people, " (:open-tasks-cnt stats)
        " open tasks, " (:backlog-cnt stats) " in backlog, "
        (:completed-cnt stats) " completed."])
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
  (get-stats :stats/activity-day-get put-fn 60)
  (get-stats :stats/tasks-day-get put-fn 60))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id      cmp-id
              :init-fn     init-fn
              :handler-map {:state/stats-tags update-stats}
              :view-fn     stats-view
              :dom-id      "stats"}))

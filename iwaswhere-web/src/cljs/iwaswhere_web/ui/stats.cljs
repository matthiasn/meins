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
  [local v]
  (fn [ev]
    (reset! local
            {:mouse-over v
             :mouse-pos  {:x (.-pageX ev)
                          :y (.-pageY ev)}})))

(defn mouse-leave-fn
  [local v]
  (fn [_ev]
    (when (= v (:mouse-over @local)) (reset! local {}))))

(defn activity-weight-chart
  [pomodoro-stats stats-key chart-h title y-scale]
  (let [local (rc/atom {})]
    (fn [pomodoro-stats stats-key chart-h title y-scale]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) pomodoro-stats)
            weights (map (fn [[k v]] [k (-> v :weight :value)]) indexed)
            weights (filter second weights)
            mapper (fn [[idx v]]
                     (str (+ 5 (* 10 idx)) "," (- chart-h (* 20 (- v 90)))))
            points (line-points weights mapper)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [chart-title title]
           (for [[idx v] indexed]
             (let [h (* y-scale (stats-key v))
                   mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)]
               ^{:key (str "pbar" stats-key idx)}
               [:rect {:x              (* 10 idx)
                       :y              (- chart-h h)
                       :width          9
                       :height         h
                       :class          (weekend-class "activity" v)
                       :on-mouse-enter mouse-enter-fn
                       :on-mouse-leave mouse-leave-fn}]))
           [path "M 0 50 l 600 0 z"]
           [path "M 0 100 l 600 0 z"]
           [path "M 0 150 l 600 0 z"]
           [path "M 0 200 l 600 0 z"]]
          [:g
           [:polyline
            {:fill :none :stroke :steelblue :stroke-width 2 :points points}]
           (for [[idx v] (filter #(:weight (second %)) indexed)]
             (let [mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)]
               ^{:key (str "weight" idx)}
               [:circle {:cx             (+ (* 10 idx) 5)
                         :cy             (- chart-h
                                            (* 20 (- (:value (:weight v)) 90)))
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

(defn tasks-chart
  [task-stats chart-h]
  (let [local (rc/atom {})]
    (fn [task-stats chart-h]
      (let [indexed (map-indexed (fn [idx [k v]] [idx v]) task-stats)]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [chart-title "Tasks opened/closed"]
           (for [[idx v] indexed]
             (let [h-tasks (* 5 (:tasks-cnt v))
                   h-done (* 5 (:done-cnt v))
                   x (* 10 idx)
                   y-tasks (- chart-h h-tasks)
                   y-done (- chart-h h-done)
                   mouse-enter-fn (mouse-enter-fn local v)
                   mouse-leave-fn (mouse-leave-fn local v)]
               ^{:key (str "tbar" (:date-string v) idx)}
               [:g {:on-mouse-enter mouse-enter-fn
                    :on-mouse-leave mouse-leave-fn}
                [:rect {:x      x
                        :y      y-tasks
                        :width  4
                        :height h-tasks
                        :class  (weekend-class "tasks" v)}]
                [:rect {:x      (+ 5 x)
                        :y      y-done
                        :width  4
                        :height h-done
                        :class  (weekend-class "done" v)}]]))
           [path "M 0 50 l 600 0 z"]
           [path "M 0 100 l 600 0 z"]
           [path "M 0 150 l 600 0 z"]
           [path "M 0 200 l 600 0 z"]]]
         (when (:mouse-over @local)
           [:div.mouse-over-info
            {:style {:top  (- (:y (:mouse-pos @local)) 20)
                     :left (+ (:x (:mouse-pos @local)) 20)}}
            [:span (:date-string (:mouse-over @local))] [:br]
            [:span "Done: " (:done-cnt (:mouse-over @local))] [:br]
            [:span "Created: " (:tasks-cnt (:mouse-over @local))] [:br]])]))))

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
      [activity-weight-chart activity-stats :total-exercise 250 "Activity/Weight" 1]
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
              :handler-map {:state/new update-stats}
              :view-fn     stats-view
              :dom-id      "stats"}))

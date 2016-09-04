(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.charts.activity :as ca]
            [iwaswhere-web.ui.charts.tasks :as ct]
            [iwaswhere-web.ui.charts.pomodoros :as cp]
            [cljsjs.moment]
            [cljs.pprint :as pp]))

(def ymd-format "YYYY-MM-DD")
(defn n-days-go [n] (.subtract (js/moment.) n "d"))
(defn n-days-go-fmt [n] (.format (n-days-go n) ymd-format))

(defn stats-view
  "Renders stats component."
  [{:keys [observed]}]
  (let [snapshot @observed
        {:keys [options pomodoro-stats activity-stats task-stats stats]} snapshot]
    [:div.stats
     [:div.charts
      [cp/pomodoro-bar-chart pomodoro-stats 250 "Pomodoros" 10]
      [ca/activity-weight-chart activity-stats 250]
      [ct/tasks-chart task-stats 250]]
     (when stats
       [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, "
        (:edge-count stats) " edges, " (count (:hashtags options)) " hashtags, "
        (count (:mentions options)) " people, " (:open-tasks-cnt stats)
        " open tasks, " (:backlog-cnt stats) " in backlog, "
        (:completed-cnt stats) " completed, "
        (:import-cnt stats) " tagged #import, "
        (:new-cnt stats) " tagged #new."])
     (when-let [ms (get-in snapshot [:timing :query])]
       [:div.stats
        (str "Query with " (get-in snapshot [:timing :count])
             " results completed in " ms ", RTT "
             (get-in snapshot [:timing :rtt]) " ms")])]))

(defn init-fn
  ""
  [{:keys [local observed put-fn]}]
  (let []))

(defn get-stats
  "Retrieves pomodoro stats for the last n days."
  [stats-key put-fn n]
  (let [days (map n-days-go-fmt (reverse (range n)))]
    (put-fn [stats-key (mapv (fn [d] {:date-string d}) days)])))

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

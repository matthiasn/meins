(ns iwaswhere-web.ui.stats
  (:require [iwaswhere-web.ui.charts.tasks :as ct]
            [iwaswhere-web.ui.charts.custom-fields :as cf]
            [iwaswhere-web.ui.charts.wordcount :as wc]
            [iwaswhere-web.ui.charts.time.durations :as cd]
            [iwaswhere-web.ui.charts.media :as m]
            [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pp]))

(def ymd-format "YYYY-MM-DD")
(defn n-days-go [n] (.subtract (js/moment.) n "d"))
(defn n-days-go-fmt [n] (.format (n-days-go n) ymd-format))

(defn stats-text
  "Renders stats text component."
  []
  (let [stats (subscribe [:stats])
        options (subscribe [:options])
        timing (subscribe [:timing])]
    (fn stats-text-render []
      [:div.stats-string
       (when stats
         [:div
          (:entry-count @stats) " entries, "
          (:node-count @stats) " nodes, "
          (:edge-count @stats) " edges, "
          (count (:hashtags @options)) " hashtags, "
          (count (:mentions @options)) " people, "
          (:open-tasks-cnt @stats) " open tasks, "
          (:started-tasks-cnt @stats) " started, "
          (:due-tasks-cnt @stats) " due, "
          (:backlog-cnt @stats) " backlog, "
          (:completed-cnt @stats) " completed, "
          (:closed-cnt @stats) " closed, "
          (:open-habits-cnt @stats) " habits, "
          (:waiting-habits-cnt @stats) " waiting, "
          (:import-cnt @stats) " tagged #import, "
          (:new-cnt @stats) " #new."])
       (when-let [ms (:query @timing)]
         [:div
          (str "Query with " (:count @timing)
               " results completed in " ms ", RTT "
               (:rtt @timing) " ms")])])))

(defn stats-view
  "Renders stats component."
  [put-fn]
  (let [chart-data (subscribe [:chart-data])]
    (fn stats-view-render [put-fn]
      (let [{:keys [pomodoro-stats task-stats wordcount-stats
                    daily-summary-stats media-stats]} @chart-data]
        [:div.stats
         [:div.charts
          [cd/durations-bar-chart pomodoro-stats 200 "Pomodoros" 5 put-fn]
          [ct/tasks-chart task-stats 250 put-fn]
          [wc/wordcount-chart wordcount-stats 150 put-fn 1000]
          [m/media-chart media-stats 150 put-fn]]]))))

(defn get-stats
  "Retrieves pomodoro stats for the last n days."
  [stats-key put-fn n]
  (let [days (map n-days-go-fmt (reverse (range n)))]
    (put-fn [:stats/get {:days (mapv (fn [d] {:date-string d}) days)
                         :type stats-key}])))

(defn update-stats
  "Request updated stats."
  [put-fn]
  (get-stats :stats/pomodoro put-fn 60)
  (get-stats :stats/custom-fields put-fn 60)
  (get-stats :stats/tasks put-fn 60)
  (get-stats :stats/wordcount put-fn 60)
  (get-stats :stats/media put-fn 60)
  (get-stats :stats/daily-summaries put-fn 60))

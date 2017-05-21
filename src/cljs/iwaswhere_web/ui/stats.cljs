(ns iwaswhere-web.ui.stats
  (:require [iwaswhere-web.ui.charts.tasks :as ct]
            [iwaswhere-web.ui.charts.custom-fields :as cf]
            [iwaswhere-web.ui.charts.wordcount :as wc]
            [iwaswhere-web.ui.charts.location :as loc]
            [iwaswhere-web.ui.charts.time.durations :as cd]
            [iwaswhere-web.ui.charts.media :as m]
            [iwaswhere-web.helpers :as h]
            [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pp]))

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
          (count (:hashtags @options)) " hashtags, "
          (count (:mentions @options)) " people, "
          (:open-tasks-cnt @stats) " open tasks, "
          (:backlog-cnt @stats) " backlog, "
          (:completed-cnt @stats) " completed, "
          (:closed-cnt @stats) " closed, "
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
      (let [{:keys [pomodoro-stats task-stats
                    wordcount-stats media-stats]} @chart-data]
        [:div.stats
         [:div.charts
          [cd/durations-bar-chart pomodoro-stats 200 5 put-fn]
          [ct/tasks-chart task-stats 250 put-fn]
          [wc/wordcount-chart wordcount-stats 150 put-fn 1000]
          [m/media-chart media-stats 150 put-fn]
          [loc/location-chart]]]))))

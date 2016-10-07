(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.charts.activity :as ca]
            [iwaswhere-web.ui.charts.tasks :as ct]
            [iwaswhere-web.ui.charts.wordcount :as wc]
            [iwaswhere-web.ui.charts.pomodoros :as cp]
            [iwaswhere-web.ui.charts.media :as m]
            [iwaswhere-web.ui.charts.daily-summaries :as ds]
            [cljsjs.moment]
            [cljs.pprint :as pp]))

(def ymd-format "YYYY-MM-DD")
(defn n-days-go [n] (.subtract (js/moment.) n "d"))
(defn n-days-go-fmt [n] (.format (n-days-go n) ymd-format))

(defn stats-view
  "Renders stats component."
  [{:keys [observed put-fn]}]
  (let [snapshot @observed
        {:keys [pomodoro-stats activity-stats task-stats wordcount-stats
                daily-summary-stats media-stats]} snapshot]
    [:div.stats
     [:div.charts
      [cp/pomodoro-bar-chart pomodoro-stats 250 "Pomodoros" 10 put-fn]
      [ct/tasks-chart task-stats 250 put-fn]
      [ds/daily-summaries-chart daily-summary-stats 200 put-fn]
      [ca/activity-weight-chart activity-stats 250 put-fn]
      [wc/wordcount-chart wordcount-stats 150 put-fn 1000]
      [m/media-chart media-stats 150 put-fn]]]))

(defn stats-text
  "Renders stats text component."
  [{:keys [observed put-fn]}]
  (let [snapshot @observed
        {:keys [options stats]} snapshot]
    [:div.stats-string
     (when stats
       [:div
        (:entry-count stats) " entries, " (:node-count stats) " nodes, "
        (:edge-count stats) " edges, " (count (:hashtags options)) " hashtags, "
        (count (:mentions options)) " people, " (:open-tasks-cnt stats)
        " open tasks, " (:backlog-cnt stats) " in backlog, "
        (:completed-cnt stats) " completed, "
        (:closed-cnt stats) " closed, "
        (:import-cnt stats) " tagged #import, "
        (:new-cnt stats) " tagged #new."])
     (when-let [ms (get-in snapshot [:timing :query])]
       [:div
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
    (put-fn [:stats/get {:days (mapv (fn [d] {:date-string d}) days)
                         :type stats-key}])))

(defn update-stats
  "Request updated stats."
  [{:keys [put-fn]}]
  (get-stats :stats/pomodoro put-fn 60)
  (get-stats :stats/activity put-fn 60)
  (get-stats :stats/tasks put-fn 60)
  (get-stats :stats/wordcount put-fn 60)
  (get-stats :stats/media put-fn 60)
  (get-stats :stats/daily-summaries put-fn 60))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id      cmp-id
              :init-fn     init-fn
              :handler-map {:state/stats-tags update-stats}
              :view-fn     stats-text
              :dom-id      "stats"}))

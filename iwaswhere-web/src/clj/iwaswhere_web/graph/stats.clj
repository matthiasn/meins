(ns iwaswhere-web.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]))

(defn get-pomodoro-day-stats
  "Get pomodoro stats for specified day."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [g (:graph current-state)
        date-string (:date-string msg-payload)
        day-nodes (gq/get-nodes-for-day g {:date-string date-string})
        day-nodes-attrs (map #(uber/attrs g %) day-nodes)
        pomodoro-nodes (filter #(= (:entry-type %) :pomodoro) day-nodes-attrs)
        stats {:date-string date-string
               :total       (count pomodoro-nodes)
               :completed   (count (filter #(= (:planned-dur %)
                                               (:completed-time %))
                                           pomodoro-nodes))
               :started     (count (filter #(and (pos? (:completed-time %))
                                                 (< (:completed-time %)
                                                    (:planned-dur %)))
                                           pomodoro-nodes))
               :total-time  (apply + (map :completed-time pomodoro-nodes))}]
    {:emit-msg (with-meta [:stats/pomo-day stats] msg-meta)}))

(defn get-activity-day-stats
  "Get activity stats for specified day."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [g (:graph current-state)
        date-string (:date-string msg-payload)
        day-nodes (gq/get-nodes-for-day g {:date-string date-string})
        day-nodes-attrs (map #(uber/attrs g %) day-nodes)
        activity-nodes (filter :activities day-nodes-attrs)
        activities (map :activities activity-nodes)
        stats {:date-string    date-string
               :total-exercise (apply + (map :total-exercise activities))}]
    {:emit-msg (with-meta [:stats/activity-day stats] msg-meta)}))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [current-state]
  {:entry-count (count (:sorted-entries current-state))
   :node-count  (count (:node-map (:graph current-state)))
   :edge-count  (count (uber/find-edges (:graph current-state) {}))})

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

(defn get-tasks-day-stats
  "Get pomodoro stats for specified day."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [g (:graph current-state)
        date-string (:date-string msg-payload)
        day-nodes (gq/get-nodes-for-day g {:date-string date-string})
        day-nodes-attrs (map #(uber/attrs g %) day-nodes)
        task-nodes (filter #(contains? (:tags %) "#task") day-nodes-attrs)
        done-nodes (filter #(contains? (:tags %) "#done") day-nodes-attrs)
        stats {:date-string date-string
               :tasks-cnt   (count task-nodes)
               :done-cnt    (count done-nodes)}]
    {:emit-msg (with-meta [:stats/tasks-day stats] msg-meta)}))

(defn get-activity-day-stats
  "Get activity stats for specified day."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [g (:graph current-state)
        date-string (:date-string msg-payload)
        day-nodes (gq/get-nodes-for-day g {:date-string date-string})
        day-nodes-attrs (map #(uber/attrs g %) day-nodes)
        weight-nodes (sort-by #(-> % :measurements :weight :value)
                              (filter #(:weight (:measurements %))
                                      day-nodes-attrs))
        activity-nodes (filter :activities day-nodes-attrs)
        activities (map :activities activity-nodes)
        stats {:date-string    date-string
               :total-exercise (apply + (map :total-exercise activities))
               :weight         (:weight (:measurements (first weight-nodes)))}]
    {:emit-msg (with-meta [:stats/activity-day stats] msg-meta)}))

(defn count-open-tasks
  [current-state]
  (count (:entries (gq/get-filtered-results
                     current-state
                     {:search-text "#task ~#done ~#backlog"
                      :tags        #{"#task"}
                      :not-tags    #{"#done" "#backlog"}
                      :n           Integer/MAX_VALUE}))))

(defn count-open-tasks-backlog
  [current-state]
  (count (:entries (gq/get-filtered-results
                     current-state
                     {:search-text "#task ~#done #backlog"
                      :tags        #{"#task" "#backlog"}
                      :not-tags    #{"#done"}
                      :n           Integer/MAX_VALUE}))))

(defn count-completed-tasks
  [current-state]
  (count (:entries (gq/get-filtered-results
                     current-state
                     {:search-text "#task #done"
                      :tags        #{"#task" "#done"}
                      :n           Integer/MAX_VALUE}))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [current-state]
  {:entry-count    (count (:sorted-entries current-state))
   :node-count     (count (:node-map (:graph current-state)))
   :edge-count     (count (uber/find-edges (:graph current-state) {}))
   :open-tasks-cnt (count-open-tasks current-state)
   :backlog-cnt    (count-open-tasks-backlog current-state)
   :completed-cnt  (count-completed-tasks current-state)})

(ns iwaswhere-web.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [matthiasn.systems-toolbox.component :as st]))
#_
(defn get-pomodoro-day-stats
  "Get pomodoro stats for specified day."
  [{:keys [current-state msg-payload msg-meta put-fn]}]
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
    {:emit-msg [:stats/pomo-day stats]}))

(defn get-pomodoro-day-stats
  "Get pomodoro stats for specified day."
  [{:keys [current-state msg-payload put-fn msg-meta]}]
  (future
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
      (put-fn (with-meta [:stats/pomo-day stats] msg-meta)))))

(defn get-tasks-day-stats
  "Get pomodoro stats for specified day."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [g (:graph current-state)
        date-string (:date-string msg-payload)
        day-nodes (gq/get-nodes-for-day g {:date-string date-string})
        day-nodes-attrs (map #(uber/attrs g %) day-nodes)
        task-nodes (filter #(contains? (:tags %) "#task") day-nodes-attrs)
        done-nodes (filter #(contains? (:tags %) "#done") day-nodes-attrs)
        closed-nodes (filter #(contains? (:tags %) "#closed") day-nodes-attrs)
        stats {:date-string date-string
               :tasks-cnt   (count task-nodes)
               :done-cnt    (count done-nodes)
               :closed-cnt  (count closed-nodes)}]
    {:emit-msg [:stats/tasks-day stats]}))

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
        activity-nodes (filter :activity day-nodes-attrs)
        activities (map :activity activity-nodes)
        stats {:date-string    date-string
               :total-exercise (apply + (map :duration-m activities))
               :weight         (:weight (:measurements (first weight-nodes)))}]
    {:emit-msg [:stats/activity-day stats]}))

(defn count-open-tasks
  [current-state]
  (count (set (:entries (gq/get-filtered-results
                          current-state
                          {:search-text "#task ~#done ~#backlog ~#closed"
                           :tags        #{"#task"}
                           :not-tags    #{"#done" "#backlog" "#closed"}
                           :n           Integer/MAX_VALUE})))))

(defn count-open-tasks-backlog
  [current-state]
  (count (set (:entries (gq/get-filtered-results
                          current-state
                          {:search-text "#task ~#done #backlog"
                           :tags        #{"#task" "#backlog"}
                           :not-tags    #{"#done"}
                           :n           Integer/MAX_VALUE})))))

(defn count-completed-tasks
  [current-state]
  (count (set (:entries (gq/get-filtered-results
                          current-state
                          {:search-text "#task #done"
                           :tags        #{"#task" "#done"}
                           :n           Integer/MAX_VALUE})))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [current-state]
  {:entry-count    (count (:sorted-entries current-state))
   :node-count     (count (:node-map (:graph current-state)))
   :edge-count     (count (uber/find-edges (:graph current-state) {}))
   :open-tasks-cnt (count-open-tasks current-state)
   :backlog-cnt    (count-open-tasks-backlog current-state)
   :completed-cnt  (count-completed-tasks current-state)})

(defn make-stats-tags
  "Generate stats and tags from current-state."
  [current-state]
  {:stats (get-basic-stats current-state)
   :hashtags (gq/find-all-hashtags current-state)
   :pvt-hashtags (gq/find-all-pvt-hashtags current-state)
   :pvt-displayed (:pvt-displayed (:cfg current-state))
   :mentions (gq/find-all-mentions current-state)
   :activities (gq/find-all-activities current-state)
   :consumption-types (gq/find-all-consumption-types current-state)})

(defn stats-tags-fn
  "Generates stats and tags (they only change on insert anyway) and initiates
   publication thereof to all connected clients."
  [{:keys [current-state put-fn]}]
  (future
    (let [stats-tags (make-stats-tags current-state)]
      (doseq [uid (keys (:client-queries current-state))]
        (put-fn (with-meta [:state/stats-tags stats-tags] {:sente-uid uid})))))
  {:new-state current-state})

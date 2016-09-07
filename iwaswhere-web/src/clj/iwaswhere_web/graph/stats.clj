(ns iwaswhere-web.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [matthiasn.systems-toolbox.component :as st]))

(defn pomodoro-mapper
  "Create mapper function for pomodoro stats"
  [g]
  (fn [d]
    (let [date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          pomo-nodes (filter #(= (:entry-type %) :pomodoro) day-nodes-attrs)
          completed (filter #(= (:planned-dur %) (:completed-time %)) pomo-nodes)
          started (filter #(and (pos? (:completed-time %))
                                (< (:completed-time %) (:planned-dur %)))
                          pomo-nodes)
          day-stats {:date-string date-string
                     :total       (count pomo-nodes)
                     :completed   (count completed)
                     :started     (count started)
                     :total-time  (apply + (map :completed-time pomo-nodes))}]
      [date-string day-stats])))

(defn tasks-mapper
  "Create mapper function for task stats"
  [g]
  (fn [d]
    (let [date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          task-nodes (filter #(contains? (:tags %) "#task") day-nodes-attrs)
          done-nodes (filter #(contains? (:tags %) "#done") day-nodes-attrs)
          closed-nodes (filter #(contains? (:tags %) "#closed") day-nodes-attrs)
          day-stats {:date-string date-string
                     :tasks-cnt   (count task-nodes)
                     :done-cnt    (count done-nodes)
                     :closed-cnt  (count closed-nodes)}]
      [date-string day-stats])))

(defn activities-mapper
  "Create mapper function for activity stats"
  [g]
  (fn [d]
    (let [date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          weight-nodes (sort-by #(-> % :measurements :weight :value)
                                (filter #(:weight (:measurements %))
                                        day-nodes-attrs))
          activity-nodes (filter :activity day-nodes-attrs)
          activities (map :activity activity-nodes)
          weight (-> weight-nodes first :measurements :weight)
          girth-vals (->> day-nodes-attrs
                          (map #(:girth (:measurements %)))
                          (filter identity)
                          (map #(+ (* (:abdominal-cm %) 10) (:abdominal-mm %))))
          girth (when (seq girth-vals) (apply min girth-vals))
          day-stats {:date-string    date-string
                     :weight         weight
                     :girth          girth
                     :total-exercise (apply + (map :duration-m activities))}]
      [date-string day-stats])))

(defn get-day-stats
  "Get stats of specified type."
  [stats-mapper msg-type]
  (fn [{:keys [current-state msg-payload put-fn msg-meta]}]
    (future
      (let [g (:graph current-state)
            stats (into {} (mapv (stats-mapper g) msg-payload))]
        (put-fn (with-meta [msg-type stats] msg-meta))))))

(defn res-count
  "Count results for specified query."
  [current-state query]
  (let [res (gq/get-filtered-results
              current-state
              (merge {:n Integer/MAX_VALUE} query))]
    (count (set (:entries res)))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [state]
  {:entry-count    (count (:sorted-entries state))
   :node-count     (count (:node-map (:graph state)))
   :edge-count     (count (uber/find-edges (:graph state) {}))
   :open-tasks-cnt (res-count state {:tags     #{"#task"}
                                     :not-tags #{"#done" "#backlog" "#closed"}})
   :backlog-cnt    (res-count state {:tags     #{"#task" "#backlog"}
                                     :not-tags #{"#done"}})
   :completed-cnt  (res-count state {:tags #{"#task" "#done"}})
   :import-cnt     (res-count state {:tags #{"#import"}})
   :new-cnt        (res-count state {:tags #{"#new"}})})

(defn make-stats-tags
  "Generate stats and tags from current-state."
  [current-state]
  {:stats             (get-basic-stats current-state)
   :hashtags          (gq/find-all-hashtags current-state)
   :pvt-hashtags      (gq/find-all-pvt-hashtags current-state)
   :pvt-displayed     (:pvt-displayed (:cfg current-state))
   :mentions          (gq/find-all-mentions current-state)
   :activities        (gq/find-all-activities current-state)
   :consumption-types (gq/find-all-consumption-types current-state)})

(defn stats-tags-fn
  "Generates stats and tags (they only change on insert anyway) and initiates
   publication thereof to all connected clients."
  [{:keys [current-state put-fn msg-meta]}]
  (future
    (let [stats-tags (make-stats-tags current-state)
          uid (:sente-uid msg-meta)]
      (put-fn (with-meta [:state/stats-tags stats-tags] {:sente-uid uid})))))

(def stats-handler-map
  {:stats/pomo-day-get     (get-day-stats pomodoro-mapper :stats/pomo-days)
   :stats/activity-day-get (get-day-stats activities-mapper :stats/activity-days)
   :stats/tasks-day-get    (get-day-stats tasks-mapper :stats/tasks-days)})

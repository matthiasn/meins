(ns iwaswhere-web.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.graph.stats.awards :as aw]
            [iwaswhere-web.graph.stats.time :as t-s]
            [iwaswhere-web.graph.stats.custom-fields :as cf]
            [iwaswhere-web.utils.misc :as u]
            [clj-time.format :as ctf]
            [matthiasn.systems-toolbox.log :as l]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]))

(defn tasks-mapper
  "Create mapper function for task stats"
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          date-string (:date-string d)
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

(defn wordcount-mapper
  "Create mapper function for wordcount stats"
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          counts (map (fn [entry] (u/count-words entry)) day-nodes-attrs)
          day-stats {:date-string date-string
                     :word-count  (apply + counts)}]
      [date-string day-stats])))

(defn media-mapper
  "Create mapper function for media stats"
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          day-stats {:date-string date-string
                     :photo-cnt   (count (filter :img-file day-nodes-attrs))
                     :audio-cnt   (count (filter :audio-file day-nodes-attrs))
                     :video-cnt   (count (filter :video-file day-nodes-attrs))}]
      [date-string day-stats])))

(defn res-count
  "Count results for specified query."
  [current-state query]
  (let [res (gq/get-filtered
              current-state
              (merge {:n Integer/MAX_VALUE} query))]
    (count (set (:entries res)))))

(defn task-summary-stats
  "Generate some very basic stats about the graph for display in UI."
  [state]
  {:open-tasks-cnt     (res-count state {:tags     #{"#task"}
                                         :not-tags #{"#done" "#backlog" "#closed"}})
   :started-tasks-cnt  (res-count state {:tags     #{"#task"}
                                         :not-tags #{"#done" "#backlog" "#closed"}
                                         :opts     #{":started"}})
   :due-tasks-cnt      (res-count state {:tags     #{"#task"}
                                         :not-tags #{"#done" "#backlog" "#closed"}
                                         :opts     #{":due"}})
   :open-habits-cnt    (res-count state {:tags     #{"#habit"}
                                         :not-tags #{"#done"}})
   :waiting-habits-cnt (res-count state {:tags     #{"#habit"}
                                         :not-tags #{"#done"}
                                         :opts     #{":waiting"}})
   :backlog-cnt        (res-count state {:tags     #{"#task" "#backlog"}
                                         :not-tags #{"#done" "#closed"}})
   :completed-cnt      (res-count state {:tags #{"#task" "#done"}})
   :closed-cnt         (res-count state {:tags #{"#task" "#closed"}})})

(defn daily-summaries-mapper
  "Create mapper function for daily summary stats"
  [current-state]
  (fn [d]
    (let [day (:date-string d)
          today? (= day (ctf/unparse (ctf/formatters :year-month-day) (t/now)))
          day-stats (if today?
                      (task-summary-stats current-state)
                      (get-in current-state [:stats :daily-summaries day]))]
      [day (merge day-stats {:date-string day})])))

(defn get-stats-fn
  "Retrieves stats of specified type. Picks the appropriate mapper function
   for the requested message type."
  [{:keys [current-state msg-payload msg-meta put-fn]}]
  (let [stats-type (:type msg-payload)
        stats-mapper (case stats-type
                       :stats/pomodoro t-s/time-mapper
                       :stats/custom-fields cf/custom-fields-mapper
                       :stats/tasks tasks-mapper
                       :stats/wordcount wordcount-mapper
                       :stats/media media-mapper
                       :stats/daily-summaries daily-summaries-mapper
                       nil)
        days (:days msg-payload)
        stats (when stats-mapper
                (into {} (mapv (stats-mapper current-state) days)))]
    (log/info stats-type (count (str stats)))
    (if stats
      (put-fn (with-meta [:stats/result {:stats stats
                                         :type  stats-type}] msg-meta))
      (l/warn "No mapper defined for" stats-type))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [state]
  (merge (task-summary-stats state)
         {:entry-count  (count (:sorted-entries state))
          :award-points (aw/award-points state)
          :node-count   (count (:node-map (:graph state)))
          :edge-count   (count (uber/find-edges (:graph state) {}))
          :import-cnt   (res-count state {:tags #{"#import"}})
          :new-cnt      (res-count state {:tags #{"#new"}})}))

(def started-tasks
  {:tags     #{"#task"}
   :not-tags #{"#done" "#backlog" "#closed"}
   :opts     #{":started"}})

(def waiting-habits
  {:tags #{"#habit"}
   :opts #{":waiting"}})

(defn make-stats-tags
  "Generate stats and tags from current-state."
  [state]
  {:stats          (get-basic-stats state)
   :hashtags       (gq/find-all-hashtags state)
   :pvt-hashtags   (gq/find-all-pvt-hashtags state)
   :started-tasks  (:entries (gq/get-filtered state started-tasks))
   :waiting-habits (:entries (gq/get-filtered state waiting-habits))
   :pvt-displayed  (:pvt-displayed (:cfg state))
   :mentions       (gq/find-all-mentions state)
   :stories        (gq/find-all-stories state)
   :locations      (gq/find-all-locations state)
   :briefings      (gq/find-all-briefings state)
   :sagas          (gq/find-all-sagas state)
   :cfg            (:cfg state)})

(defn stats-tags-fn
  "Generates stats and tags (they only change on insert anyway) and initiates
   publication thereof to all connected clients."
  [{:keys [current-state put-fn msg-meta]}]
  (future
    (let [stats-tags (make-stats-tags current-state)
          uid (:sente-uid msg-meta)]
      (put-fn (with-meta [:state/stats-tags stats-tags] {:sente-uid uid}))))
  {})

(def stats-handler-map
  {:stats/get            get-stats-fn
   :state/stats-tags-get stats-tags-fn})

(ns iwaswhere-web.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.utils.misc :as u]
            [clj-time.format :as ctf]
            [matthiasn.systems-toolbox.log :as l]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]))

(defn time-by-sagas
  "Calculate time spent per saga, plus time not assigned to any saga."
  [g by-story]
  (let [stories (gq/find-all-stories {:graph g})
        saga-reducer (fn [acc [k v]]
                       (let [saga (get-in stories [k :linked-saga] :no-saga)]
                         (update-in acc [saga] #(+ v (or % 0)))))]
    (reduce saga-reducer {} by-story)))

(defn manually-logged
  "Calculates summed duration and returns it when entry is either not for a
   different day, or, if so, when date string from query is equal to the
   referencenced day. Otherwise returns zero."
  [entry date-string]
  (let [manual (gq/summed-durations entry)]
    (if-let [for-day (:for-day entry)]
      (let [ymd (subs for-day 0 10)]
        (if (= date-string ymd) manual 0))
      manual)))

(defn time-by-stories
  "Calculate time spent per story, plus total time."
  [g nodes date-string]
  (let [stories (gq/find-all-stories {:graph g})
        story-reducer (fn [acc entry]
                        (let [comment-for (:comment-for entry)
                              parent (when comment-for (uc/attrs g comment-for))
                              story (or (:linked-story parent)
                                        (:linked-story entry)
                                        :no-story)
                              acc-time (get acc story 0)
                              completed (get entry :completed-time 0)
                              manual (manually-logged entry date-string)
                              summed (+ acc-time completed manual)]
                          (if (pos? summed)
                            (assoc-in acc [story] summed)
                            acc)))
        by-ts-reducer (fn [acc entry]
                        (let [comment-for (:comment-for entry)
                              parent (when comment-for (uc/attrs g comment-for))
                              story (or (:linked-story parent)
                                        (:linked-story entry)
                                        :no-story)
                              acc-time (get acc story 0)
                              story-name (:story-name (get-in stories [story]))
                              ts (:timestamp entry)
                              completed (get entry :completed-time 0)
                              manual (manually-logged entry date-string)
                              summed (+ acc-time completed manual)]
                          (if (pos? summed)
                            (assoc-in acc [ts] {:story-name story-name
                                                :summed     summed
                                                :completed  completed
                                                :manual     manual})
                            acc)))
        by-story (reduce story-reducer {} nodes)
        by-ts (reduce by-ts-reducer {} nodes)]
    {:total-time    (apply + (map second by-story))
     :time-by-ts    by-ts
     :time-by-story by-story
     :time-by-saga  (time-by-sagas g by-story)}))

(defn pomodoro-mapper
  "Create mapper function for pomodoro stats"
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          pomo-nodes (filter #(= (:entry-type %) :pomodoro) day-nodes-attrs)
          completed (filter #(= (:planned-dur %) (:completed-time %)) pomo-nodes)
          started (filter #(and (pos? (:completed-time %))
                                (< (:completed-time %) (:planned-dur %)))
                          pomo-nodes)
          day-stats (merge {:date-string date-string
                            :total       (count pomo-nodes)
                            :completed   (count completed)
                            :started     (count started)}
                           (time-by-stories g day-nodes-attrs date-string))]
      [date-string day-stats])))

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

(defn custom-fields-mapper
  "Creates mapper function for custom field stats. Takes current state. Returns
   function that takes date string, such as '2016-10-10', and returns map with
   results for the defined custom fields, plus the date string. Performs
   operation specified for field, such as sum, min, max."
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          custom-fields (:custom-fields (:cfg current-state))
          custom-field-stats-def (into {} (map (fn [[k v]] [k (:fields v)])
                                               custom-fields))
          date-string (:date-string d)
          day-nodes (gq/get-nodes-for-day g {:date-string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          nodes (filter :custom-fields day-nodes-attrs)

          stats-mapper
          (fn [[k fields]]
            (let [sum-mapper
                  (fn [[field v]]
                    (let [path [:custom-fields k field]
                          val-mapper #(get-in % path)
                          op (if (= :number (:type (:cfg v)))
                               (case (:agg v)
                                 :min #(when (seq %) (apply min %))
                                 :max #(when (seq %) (apply max %))
                                 :mean #(when (seq %) (/ (apply + %) (count %)))
                                 :none nil
                                 #(apply + %))
                               nil)
                          res (mapv val-mapper nodes)]
                      [field (when op
                               (try (op (filter identity res))
                                    (catch Exception e (log/error e res))))]))]
              [k (into {} (mapv sum-mapper fields))]))
          day-stats (into {:date-string date-string}
                          (mapv stats-mapper custom-field-stats-def))]
      [date-string day-stats])))

(defn get-stats-fn
  "Retrieves stats of specified type. Picks the appropriate mapper function
   for the requested message type."
  [{:keys [current-state msg-payload msg-meta put-fn]}]
  (let [stats-type (:type msg-payload)
        stats-mapper (case stats-type
                       :stats/pomodoro pomodoro-mapper
                       :stats/custom-fields custom-fields-mapper
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

(defn award-points
  "Counts awarded points."
  [current-state]
  (let [q {:tags #{"#habit"}}]
    (->> (gq/get-filtered current-state (merge {:n Integer/MAX_VALUE} q))
         :entries-map
         vals
         (map :habit)
         (filter :done)
         (map :points)
         (filter identity)
         (apply +))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [state]
  (merge (task-summary-stats state)
         {:entry-count  (count (:sorted-entries state))
          :award-points (award-points state)
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

(defn mk-daily-summary
  "Gathers daily summary stats at the beginning of each day."
  [state day-snapshot day-node]
  (let [day-stats (task-summary-stats day-snapshot)
        day (t/date-time (:year day-node) (:month day-node) (:day day-node))
        day-string (ctf/unparse (ctf/formatters :year-month-day) day)]
    (update-in state [:stats :daily-summaries day-string] merge day-stats)))

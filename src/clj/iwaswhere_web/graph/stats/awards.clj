(ns iwaswhere-web.graph.stats.awards
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.utils.misc :as u]
            [clj-time.format :as ctf]
            [matthiasn.systems-toolbox.log :as l]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]
            [clojure.pprint :as pp]))

(defn award-points-by
  "Counts awarded points."
  [k initial entries]
  (let [done-entries (filter #(and (-> % k :done) (-> % k :points)) entries)
        skipped-entries (filter #(and (-> % k :skipped) (-> % k :points)) entries)
        by-day-fn (fn [acc entry]
                    (let [entitity (k entry)
                          completion (:completion-ts entitity)
                          points (:points entitity)]
                      (if (and (pos? points) (seq completion) (:done entitity))
                        (let [completion-day (subs completion 0 10)]
                          (update-in acc [:by-day completion-day k] #(+ (or % 0) points)))
                        acc)))
        by-day (reduce by-day-fn initial done-entries)
        total (->> done-entries
                   (map k)
                   (filter :done)
                   (map :points)
                   (filter identity)
                   (apply +))
        total-skipped (->> skipped-entries
                           (map k)
                           (filter :skipped)
                           (map (fn [entry]
                                  (let [penalty (:penalty entry)
                                        points (:points entry)]
                                    (if (and penalty (pos-int? penalty))
                                      penalty
                                      points))))
                           (filter identity)
                           (apply +))]
    (-> by-day
        (update-in [:total] #(+ (or % 0) total))
        (update-in [:total-skipped] #(+ (or % 0) total-skipped)))))

(defn claimed-points
  "Counts claimed award points."
  [current-state]
  (let [res (gq/get-filtered
              current-state {:tags #{"#reward"} :n Integer/MAX_VALUE})
        entries (vals (:entries-map res))]
    (->> entries
         (map :reward)
         (filter :claimed)
         (map :points)
         (filter identity)
         (apply +))))

(defn award-points
  "Counts awarded points."
  [current-state]
  (let [q {:tags #{"#habit"} :n Integer/MAX_VALUE}
        entries (->> (gq/get-filtered current-state q) :entries-map vals)
        by-habit (award-points-by :habit {:total 0} entries)
        q2 {:tags #{"#task"} :opts #{":done"} :n Integer/MAX_VALUE}
        by-task (vals (:entries-map (gq/get-filtered current-state q2)))
        by-habit-and-task (award-points-by :task by-habit by-task)
        claimed (claimed-points current-state)]
    (merge by-habit-and-task
           {:claimed claimed})))

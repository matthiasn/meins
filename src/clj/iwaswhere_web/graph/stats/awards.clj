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

(defn award-point-stats
  "Counts awarded points."
  [entries]
  (let [by-day-mapper (fn [acc entry]
                        (let [habit (:habit entry)
                              completion (:completion-ts habit)
                              completion-day (subs completion 0 10)
                              points (:points habit)]
                          (if (and (pos? points) (:done habit))
                            (update-in acc [completion-day] #(+ (or % 0) points))
                            acc)))
        by-day (reduce by-day-mapper {} entries)
        total (->> entries
                   (map :habit)
                   (filter :done)
                   (map :points)
                   (filter identity)
                   (apply +))]
    {:total  total
     :by-day by-day}))

(defn award-points
  "Counts awarded points."
  [current-state]
  (let [q {:tags #{"#habit"}
           :n    Integer/MAX_VALUE}
        entries (->> (gq/get-filtered current-state q) :entries-map vals)]
    (award-point-stats (filter #(and (-> % :habit :done)
                                     (-> % :habit :points)) entries))))

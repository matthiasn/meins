(ns iwaswhere-web.graph.stats.awards
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.utils.misc :as u]
            [clj-time.format :as ctf]
            [matthiasn.systems-toolbox.log :as l]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]))

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

(ns iwaswhere-web.graph.stats.questionnaires
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.graph.stats.awards :as aw]
            [iwaswhere-web.graph.stats.time :as t-s]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.ui.questionnaires :as q]
            [clj-time.format :as ctf]
            [matthiasn.systems-toolbox.log :as l]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]))


(defn questionnaires
  "Calculates questionnaire scores."
  [current-state]
  (let [n Integer/MAX_VALUE
        res (gq/get-filtered current-state {:tags #{"#PANAS"} :n n})
        entries-map (:entries-map res)
        cfg (-> current-state :cfg :questionnaires :items :panas)
        score-mapper (fn [[ts entry]]
                       (let [path [:questionnaires :panas]
                             scores (->> (q/scores entry path cfg)
                                         (map (fn [[k v]] [k (:score v)]))
                                         (into {}))]
                         [ts scores]))
        scores (into {} (mapv score-mapper entries-map))]
    {:panas scores}))

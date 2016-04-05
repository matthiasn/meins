(ns iwaswhere-web.graph
  (:require [ubergraph.core :as uber]
            [clojure.pprint :as pp]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.tools.logging :as log]))

(defn extract-sorted-entries
  "Extracts nodes and their properties in descending timestamp order by looking for node by mapping
  over the sorted set and extracting attributes for each node.
  Warns when node not in graph. (debugging, should never happen)"
  [current-state]
  {:entries (into [] (map (fn [n]
                            (let [g (:graph current-state)]
                              (if (uber/has-node? g n)
                                (uber/attrs (:graph current-state) n)
                                (log/warn "Cannot find node: " n))))
                          (:sorted-entries current-state)))})

(defn add-hashtags
  "Add hashtag edges to graph for a new entry. When a hashtag exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [tags (:tags entry)]
    (reduce (fn [acc tag]
              (-> acc
                  (uber/add-nodes :hashtags  {:tag tag})
                  (uber/add-edges [{:tag tag} (:timestamp entry) {:relationship :CONTAINS}]
                                  [:hashtags {:tag tag} {:relationship :IS}])))
            graph
            tags)))

(defn add-timeline-tree
  "Adds graph nodes for year, month and day of entry and connects those if they don't exist.
  In any case, connects new entry node to the entry node of the matching :timeline/day node."
  [graph entry]
  (let [dt (ctc/from-long (:timestamp entry))
        year (ct/year dt)
        month (ct/month dt)
        year-node {:type :timeline/year :year year}
        month-node {:type :timeline/month :year year :month month}
        day-node {:type :timeline/day :year year :month month :day (ct/day dt)}]
    (-> graph
        (uber/add-nodes year-node month-node day-node)
        (uber/add-edges [year-node month-node]
                        [month-node day-node]
                        [day-node (:timestamp entry) {:relationship :DATE}]))))

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries sorted by timestamp."
  [current-state ts entry]
  (-> current-state
      (update-in [:graph] #(uber/add-nodes-with-attrs % [ts entry]))
      (update-in [:graph] add-hashtags entry)
      (update-in [:graph] add-timeline-tree entry)
      (update-in [:sorted-entries] conj ts)))


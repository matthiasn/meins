(ns iwaswhere-web.graph
  (:require [ubergraph.core :as uber]
            [clojure.pprint :as pp]
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

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries sorted by timestamp."
  [current-state ts entry]
  (-> current-state
      (update-in [:graph] #(uber/add-nodes-with-attrs % [ts entry]))
      (update-in [:sorted-entries] conj ts)))

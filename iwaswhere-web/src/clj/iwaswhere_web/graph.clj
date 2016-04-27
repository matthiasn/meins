(ns iwaswhere-web.graph
  (:require [ubergraph.core :as uber]
            [clojure.pprint :as pp]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags in the new entry are contained in
  the filtered entry. This filters entries so that only entries that are relevant to the new
  entry are shown."
  ; TODO: also enable OR filter
  [q]
  (fn [entry]
    (let [entry-tags (set (map s/lower-case (:tags entry)))
          q-tags (set (map s/lower-case (:tags q)))
          entry-mentions (set (map s/lower-case (:mentions entry)))
          q-mentions (set (map s/lower-case (:mentions q)))
          match? (or (and (empty? q-tags) (empty? q-mentions))
                     (seq (set/intersection q-tags entry-tags))
                     (seq (set/intersection q-mentions entry-mentions)))]
      ;      (set/subset? new-entry-tags entry-tags)
      match?)))

(defn extract-sorted-entries
  "Extracts nodes and their properties in descending timestamp order by looking for node by mapping
  over the sorted set and extracting attributes for each node.
  Warns when node not in graph. (debugging, should never happen)"
  [current-state]
  (into [] (map (fn [n]
                  (let [g (:graph current-state)]
                    (if (uber/has-node? g n)
                      (uber/attrs (:graph current-state) n)
                      (log/warn "Cannot find node: " n))))
                (:sorted-entries current-state))))

(defn find-all-hashtags
  "Finds all hashtags used in entries by finding the edges that originate from the
  :hashtags node."
  [current-state]
  (let [g (:graph current-state)]
    (set (map #(-> % :dest :tag) (uber/find-edges g {:src :hashtags})))))

(defn find-all-mentions
  "Finds all hashtags used in entries by finding the edges that originate from the
  :hashtags node."
  [current-state]
  (let [g (:graph current-state)]
    (set (map #(-> % :dest :mention) (uber/find-edges g {:src :mentions})))))

(defn get-basic-stats
  "Generate some very basic stats about the graph size for display in UI."
  [current-state]
  {:node-count (count (:node-map (:graph current-state)))
   :edge-count (count (uber/find-edges (:graph current-state) {}))})

(defn get-filtered-results
  "Retrieve items to show in UI, also deliver all hashtags for autocomplete and
  some basic stats."
  [current-state msg-payload]
  (let [entries (take 100 (filter (entries-filter-fn msg-payload)
                                 (extract-sorted-entries current-state)))]
    {:entries  entries
     :hashtags (find-all-hashtags current-state)
     :mentions (find-all-mentions current-state)
     :stats    (get-basic-stats current-state)}))

(defn add-hashtags
  "Add hashtag edges to graph for a new entry. When a hashtag exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [tags (:tags entry)]
    (reduce (fn [acc tag]
              (-> acc
                  (uber/add-nodes :hashtags {:tag tag})
                  (uber/add-edges [{:tag tag} (:timestamp entry) {:relationship :CONTAINS}]
                                  [:hashtags {:tag tag} {:relationship :IS}])))
            graph
            tags)))

(defn add-mentions
  "Add mention edges to graph for a new entry. When a mentioned person exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [mentions (:mentions entry)]
    (reduce (fn [acc mention]
              (-> acc
                  (uber/add-nodes :mentions {:mention mention})
                  (uber/add-edges [{:mention mention} (:timestamp entry) {:relationship :CONTAINS}]
                                  [:mentions {:mention mention} {:relationship :IS}])))
            graph
            mentions)))

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
      (update-in [:graph] add-mentions entry)
      (update-in [:graph] add-timeline-tree entry)
      (update-in [:sorted-entries] conj ts)))

(defn remove-node
  "Removes node from graph and sorted set."
  [current-state ts]
  (-> current-state
      (update-in [:graph] #(uber/remove-nodes % ts))
      (update-in [:sorted-entries] disj ts)))


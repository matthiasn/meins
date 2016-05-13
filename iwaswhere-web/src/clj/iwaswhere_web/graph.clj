(ns iwaswhere-web.graph
  (:require [ubergraph.core :as uber]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags in the query are contained in
  the filtered entry, and none of the not-tags."
  [q]
  (fn [entry]
    (let [entry-tags (set (map s/lower-case (:tags entry)))
          entry-comments-tags (apply set/union (map :tags (:comments entry)))
          entries-and-comments-tags (set/union entry-tags entry-comments-tags)
          q-tags (set (map s/lower-case (:tags q)))
          entry-mentions (set (map s/lower-case (:mentions entry)))
          q-mentions (set (map s/lower-case (:mentions q)))
          match? (or (and (empty? q-tags) (empty? q-mentions))
                     (set/subset? #{"#new-entry"} entries-and-comments-tags)
                     ; all tags are contained in entry or comment, and none of the not-tags
                     (and (set/subset? q-tags entries-and-comments-tags)
                          (empty? (set/intersection (:not-tags q) entries-and-comments-tags)))
                     (seq (set/intersection q-mentions entry-mentions)))]
      match?)))

(defn extract-sorted-entries
  "Extracts nodes and their properties in descending timestamp order by looking for node by mapping
  over the sorted set and extracting attributes for each node.
  Warns when node not in graph. (debugging, should never happen)"
  [current-state]
  (vec (map (fn [n]
              (let [g (:graph current-state)]
                (if (uber/has-node? g n)
                  (let [attrs (uber/attrs g n)
                        comment-edges (flatten (uber/find-edges g {:dest n :relationship :COMMENT}))
                        comments (->> comment-edges
                                      (remove :mirror?)
                                      (map #(uber/attrs g (:src %)))
                                      (sort-by :timestamp))
                        entry (merge attrs {:comments comments})]
                    entry)
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
  {:entry-count (count (:sorted-entries current-state))
   :node-count  (count (:node-map (:graph current-state)))
   :edge-count  (count (uber/find-edges (:graph current-state) {}))})

(defn get-filtered-results
  "Retrieve items to show in UI, also deliver all hashtags for autocomplete and
  some basic stats."
  [current-state msg-payload]
  (let [entries (take 500 (filter (entries-filter-fn msg-payload)
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

(defn add-parent-ref
  "Adds an edge to parent node when :comment-for key on the entry exists."
  [graph entry]
  (if-let [comment-for (:comment-for entry)]
    (uber/add-edges graph [(:timestamp entry) comment-for {:relationship :COMMENT}])
    graph))

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries sorted by timestamp."
  [current-state ts entry]
  (-> current-state
      (update-in [:graph] #(uber/add-nodes-with-attrs % [ts entry]))
      (update-in [:graph] add-hashtags entry)
      (update-in [:graph] add-mentions entry)
      (update-in [:graph] add-timeline-tree entry)
      (update-in [:graph] add-parent-ref entry)
      (update-in [:sorted-entries] conj ts)))

(defn remove-node
  "Removes node from graph and sorted set."
  [current-state ts]
  (-> current-state
      (update-in [:graph] #(uber/remove-nodes % ts))
      (update-in [:sorted-entries] disj ts)))

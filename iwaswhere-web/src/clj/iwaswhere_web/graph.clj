(ns iwaswhere-web.graph
  "this namespace manages interactions with the graph data structure, which
  holds all entries and their connections."
  (:require [ubergraph.core :as uber]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clj-time.format :as timef]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags and mentions in the query are
  contained in the filtered entry or any of it's comments, and none of the not-tags.
  Also allows filtering per day."
  [q]
  (fn [entry]
    (let [local-fmt (timef/with-zone (timef/formatters :year-month-day) (ct/default-time-zone))
          entry-day (timef/unparse local-fmt (ctc/from-long (:timestamp entry)))
          q-day (:date-string q)
          day-match? (= q-day entry-day)

          q-timestamp (:timestamp q)
          q-ts-match? (= q-timestamp (str (:timestamp entry)))

          q-tags (set (map s/lower-case (:tags q)))
          q-not-tags (set (map s/lower-case (:not-tags q)))
          q-mentions (set (map s/lower-case (:mentions q)))

          entry-tags (set (map s/lower-case (:tags entry)))
          entry-comments-tags (apply set/union (map :tags (:comments entry)))
          tags (set (map s/lower-case (set/union entry-tags entry-comments-tags)))

          entry-mentions (set (map s/lower-case (:mentions entry)))
          entry-comments-mentions (apply set/union (map :mentions (:comments entry)))
          mentions (set (map s/lower-case (set/union entry-mentions entry-comments-mentions)))

          match? (or (and (set/subset? q-tags tags)
                          (empty? (set/intersection q-not-tags tags))
                          (or (empty? q-mentions)
                              (seq (set/intersection q-mentions mentions)))
                          (or day-match? (empty? q-day))
                          (or q-ts-match? (empty? q-timestamp))))]
      match?)))

(defn compare-w-upvotes
  "Sort comparator which considers upvotes first, and, if those are equal, the timestamp second."
  [x y]
  (let [upvotes-x (get x :upvotes 0)
        upvotes-y (get y :upvotes 0)]
    (if-not (= upvotes-x upvotes-y)
      (. clojure.lang.Util (compare upvotes-y upvotes-x))
      (if (pos? upvotes-x)                                  ; when entries have upvotes, sort oldest on top
        (. clojure.lang.Util (compare (:timestamp x) (:timestamp y)))
        (. clojure.lang.Util (compare (:timestamp y) (:timestamp x)))))))

(defn get-comments
  "Extract all comments for entry."
  [entry g n]
  (merge entry
         {:comments (->> (flatten (uber/find-edges g {:dest n :relationship :COMMENT}))
                         (remove :mirror?)
                         (map #(uber/attrs g (:src %)))
                         (sort-by :timestamp))}))

(defn get-linked-entries
  "Extract all linked entries for entry, including their comments."
  [entry g n sort-by-upvotes?]
  (let [linked (->> (flatten (uber/find-edges g {:src n :relationship :LINKED}))
                    (remove :mirror?)
                    (map #(uber/attrs g (:dest %)))
                    (sort-by :timestamp)
                    (map (fn [linked-entry]
                           (get-comments linked-entry g (:timestamp linked-entry)))))]
    (merge entry {:linked-entries-list (if sort-by-upvotes? (sort compare-w-upvotes linked) linked)})))

(defn extract-sorted-entries
  "Extracts nodes and their properties in descending timestamp order by looking for node by mapping
  over the sorted set and extracting attributes for each node.
  Warns when node not in graph. (debugging, should never happen)"
  [current-state query]
  (let [sort-by-upvotes? (:sort-by-upvotes query)
        mapper-fn (fn [n]
                    (let [g (:graph current-state)]
                      (if (uber/has-node? g n)
                        (-> (uber/attrs g n)
                            (get-comments g n)
                            (get-linked-entries g n sort-by-upvotes?))
                        (log/warn "Cannot find node: " n))))
        entries (map mapper-fn (:sorted-entries current-state))]
    (if sort-by-upvotes?
      (sort compare-w-upvotes entries)
      entries)))

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
  [current-state query]
  (let [n (:n query)
        entries (take n (filter (entries-filter-fn query) (extract-sorted-entries current-state query)))]
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

(defn add-linked
  "Add mention edges to graph for a new entry. When a mentioned person exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [linked-entries (:linked-entries entry)]
    (reduce (fn [acc linked-entry]
              (-> acc
                  (uber/add-edges [(:timestamp entry) linked-entry {:relationship :LINKED}])))
            graph
            linked-entries)))

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries sorted by timestamp."
  [current-state ts entry]
  (-> current-state
      (update-in [:graph] #(uber/add-nodes-with-attrs % [ts entry]))
      (update-in [:graph] add-hashtags entry)
      (update-in [:graph] add-mentions entry)
      (update-in [:graph] add-linked entry)
      (update-in [:graph] add-timeline-tree entry)
      (update-in [:graph] add-parent-ref entry)
      (update-in [:sorted-entries] conj ts)))

(defn remove-unused-tags
  "Checks for orphan tags and removes them. Orphan tags would occur when deleting the last entry
  that contains a specific tag. Takes the state, a list of tags, and the tag type, such as :tags
  or :mentions."
  [state tags k]
  (reduce (fn [state t]
            (if (empty? (uber/find-edges (:graph state) {:src {k t} :relationship :CONTAINS}))
              (update-in state [:graph] #(uber/remove-nodes % {k t}))
              state))
          state
          tags))

(defn remove-node
  "Removes node from graph and sorted set."
  [current-state ts]
  (let [g (:graph current-state)]
    (if (uber/has-node? g ts)
      (let [entry (uber/attrs g ts)]
        (-> current-state
            (update-in [:graph] #(uber/remove-nodes % ts))
            (update-in [:sorted-entries] disj ts)
            (remove-unused-tags (:mentions entry) :mention)
            (remove-unused-tags (:tags entry) :tag)))
      (do (log/warn "Cannot find node: " ts)
          current-state))))

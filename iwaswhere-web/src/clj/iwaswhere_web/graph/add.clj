(ns iwaswhere-web.graph.add
  "Functions for adding new entries."
  (:require [ubergraph.core :as uber]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

(defn add-hashtags
  "Add hashtag edges to graph for a new entry. When a hashtag exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [tags (:tags entry)]
    (reduce (fn [acc tag]
              (let [ltag (s/lower-case tag)]
                (-> acc
                    (uber/add-nodes :hashtags)
                    (uber/add-nodes-with-attrs [{:tag ltag} {:val tag}])
                    (uber/add-edges [{:tag ltag} (:timestamp entry) {:relationship :CONTAINS}]
                                    [:hashtags {:tag ltag} {:relationship :IS}]))))
            graph
            tags)))

(defn add-mentions
  "Add mention edges to graph for a new entry. When a mentioned person exists already, an edge to
  the existing node will be added, otherwise a new hashtag node will be created."
  [graph entry]
  (let [mentions (:mentions entry)]
    (reduce (fn [acc mention]
              (let [lmention (s/lower-case mention)]
                (-> acc
                    (uber/add-nodes :mentions)
                    (uber/add-nodes-with-attrs [{:mention lmention} {:val mention}])
                    (uber/add-edges
                      [{:mention lmention} (:timestamp entry) {:relationship :CONTAINS}]
                      [:mentions {:mention lmention} {:relationship :IS}]))))
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
              (if (uber/has-node? graph linked-entry)
                (uber/add-edges acc [(:timestamp entry) linked-entry {:relationship :LINKED}])
                (do (log/warn "Linked node does not exist, skipping" linked-entry)
                    acc)))
            graph
            linked-entries)))

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
      (do (log/warn "remove-node cannot find node: " ts)
          current-state))))

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries sorted by timestamp."
  [current-state ts entry]
  (let [graph (:graph current-state)
        old-entry (when (uber/has-node? graph ts) (uber/attrs graph ts))
        tags-not-in-new (set/difference (:tags old-entry) (:tags entry))
        mentions-not-in-new (set/difference (:mentions old-entry) (:mentions entry))
        remove-tag-edges (fn [g tags k]
                           (reduce #(uber/remove-edges %1 [(:timestamp entry) {k %2}]) g tags))]
    (-> current-state
        (update-in [:graph] #(uber/add-nodes-with-attrs % [ts (merge old-entry entry)]))
        (update-in [:graph] remove-tag-edges tags-not-in-new :tag)
        (update-in [:graph] remove-tag-edges mentions-not-in-new :mention)
        (remove-unused-tags tags-not-in-new :tag)
        (remove-unused-tags mentions-not-in-new :mention)
        (update-in [:graph] add-hashtags entry)
        (update-in [:graph] add-mentions entry)
        (update-in [:graph] add-linked entry)
        (update-in [:graph] add-timeline-tree entry)
        (update-in [:graph] add-parent-ref entry)
        (update-in [:sorted-entries] conj ts))))

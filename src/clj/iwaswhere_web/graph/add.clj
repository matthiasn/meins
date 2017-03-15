(ns iwaswhere-web.graph.add
  "Functions for adding new entries."
  (:require [ubergraph.core :as uc]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clj-time.format :as ctf]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.graph.query :as gq]
            [iwaswhere-web.specs :as specs]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [iwaswhere-web.graph.stats :as gs]))

(defn add-hashtags
  "Add hashtag edges to graph for a new entry. When a hashtag exists already,
   an edge to the existing node will be added, otherwise a new hashtag node will
   be created.
   When any of the private tags occur, the entry is considered private, and all
   the tags that are not known to be public will be added to the private tags
   in the graph."
  [current-state entry]
  (let [graph (:graph current-state)
        cfg (:cfg current-state)
        tags (set (:tags entry))
        pvt-tags (set/union (:pvt-displayed cfg) (:pvt-tags cfg))
        pvt-entry? (seq (set/intersection tags pvt-tags))
        tag-add-fn
        (fn [g tag]
          (let [ltag (s/lower-case tag)
                public-tag? (uc/has-node? graph {:tag ltag})
                pvt-tag? (and pvt-entry?
                              (not public-tag?))
                ht-parent (if pvt-tag? :pvt-hashtags :hashtags)
                tag-type (if pvt-tag? :ptag :tag)]
            (-> g
                (uc/add-nodes ht-parent)
                (uc/add-nodes-with-attrs [{tag-type ltag} {:val tag}])
                (uc/add-edges
                  [{tag-type ltag} (:timestamp entry)
                   {:relationship :CONTAINS}]
                  [ht-parent {tag-type ltag} {:relationship :IS}]))))]
    (assoc-in current-state [:graph] (reduce tag-add-fn graph tags))))

(defn add-mentions
  "Add mention edges to graph for a new entry. When a mentioned person exists
   already, an edge to the existing node will be added, otherwise a new hashtag
   node will be created."
  [graph entry]
  (let [mentions (:mentions entry)]
    (reduce
      (fn [acc mention]
        (let [lmention (s/lower-case mention)]
          (-> acc
              (uc/add-nodes :mentions)
              (uc/add-nodes-with-attrs [{:mention lmention} {:val mention}])
              (uc/add-edges
                [{:mention lmention} (:timestamp entry) {:relationship :CONTAINS}]
                [:mentions {:mention lmention} {:relationship :IS}]))))
      graph
      mentions)))

(defn local-dt
  "Return joda dt in local timezone for given entry."
  [entry]
  (-> (:timestamp entry)
      (ctc/from-long)
      (ct/to-time-zone (ct/default-time-zone))))

(defn add-timeline-tree
  "Adds graph nodes for year, month and day of entry and connects those if they
   don't exist. In any case, connects new entry node to the entry node of the
   matching :timeline/day node."
  [state entry]
  (let [g (:graph state)
        dt (local-dt entry)
        year (ct/year dt)
        month (ct/month dt)
        year-node {:type :timeline/year :year year}
        month-node {:type :timeline/month :year year :month month}
        day-node {:type :timeline/day :year year :month month :day (ct/day dt)}
        day-node-exists? (uc/has-node? g day-node)]
    (assoc-in state [:graph] (-> g
                                 (uc/add-nodes year-node month-node day-node)
                                 (uc/add-edges
                                   [year-node month-node]
                                   [month-node day-node]
                                   [day-node
                                    (:timestamp entry)
                                    {:relationship :DATE}])))))

(defn add-daily-summary
  "Adds graph nodes for year, month and day of entry and connects those if they
   don't exist."
  [state entry startup?]
  (let [g (:graph state)
        dt (local-dt entry)
        year (ct/year dt)
        month (ct/month dt)
        day-node {:type :timeline/day :year year :month month :day (ct/day dt)}
        day-node-exists? (uc/has-node? g day-node)]
    (if (and (not day-node-exists?) (>= (:year day-node) 2016))
      (if startup?
        (assoc-in state [:days-to-summarize day-node] state)
        (gs/mk-daily-summary state state day-node))
      state)))

(defn add-parent-ref
  "Adds an edge to parent node when :comment-for key on the entry exists."
  [graph entry]
  (if-let [comment-for (:comment-for entry)]
    (uc/add-edges graph [(:timestamp entry) comment-for {:relationship :COMMENT}])
    graph))

(defn add-linked
  "Add mention edges to graph for a new entry. When a mentioned person exists
   already, an edge to the existing node will be added, otherwise a new hashtag
   node will be created."
  [graph entry]
  (let [linked-entries (:linked-entries entry)]
    (reduce (fn [acc linked-entry]
              (let [with-linked (if (uc/has-node? acc linked-entry)
                                  acc (uc/add-nodes acc linked-entry))]
                (uc/add-edges with-linked [(:timestamp entry) linked-entry
                                           {:relationship :LINKED}])))
            graph
            linked-entries)))

(defn add-linked-visit
  "Adds linked entry when the entry has been captured during a visit."
  [g entry]
  (let [{:keys [arrival-ts departure-ts]} (u/visit-timestamps entry)]
    (if departure-ts
      (let [ts (:timestamp entry)
            ts-dt (c/from-long ts)
            q {:date-string (ctf/unparse (ctf/formatters :year-month-day) ts-dt)}
            same-day-entry-ids (gq/get-nodes-for-day g q)
            same-day-entries (mapv #(uc/attrs g %) same-day-entry-ids)
            filter-fn (fn [other-entry]
                        (let [other-ts (:timestamp other-entry)]
                          (and (< other-ts departure-ts)
                               (> other-ts arrival-ts))))
            matching-entries (filterv filter-fn same-day-entries)
            matching-entry-ids (mapv :timestamp matching-entries)
            reducing-fn (fn [g match]
                          (uc/add-edges g [ts match {:relationship :LINKED}]))]
        (reduce reducing-fn g matching-entry-ids))
      g)))

(defn remove-unused-tags
  "Checks for orphan tags and removes them. Orphan tags would occur when
   deleting the last entry that contains a specific tag. Takes the state, a set
   of tags, and the tag type, such as :tags or :mentions."
  [graph tags k]
  (reduce
    (fn [g tag]
      (let [ltag (s/lower-case tag)]
        (if (empty? (uc/find-edges g {:src {k ltag} :relationship :CONTAINS}))
          (uc/remove-nodes g {k ltag})
          g)))
    graph
    tags))

(defn remove-node
  "Removes node from graph and sorted set if node for specified timestamp
   exists."
  [current-state ts]
  (let [g (:graph current-state)]
    (if (uc/has-node? g ts)
      (let [entry (uc/attrs g ts)]
        (-> current-state
            (update-in [:graph] uc/remove-nodes ts)
            (update-in [:sorted-entries] disj ts)
            (update-in [:graph] remove-unused-tags (:mentions entry) :mention)
            (update-in [:graph] remove-unused-tags (:tags entry) :tag)
            (update-in [:graph] remove-unused-tags (:tags entry) :ptag)))
      (do (log/warn "remove-node cannot find node: " ts)
          current-state))))

(defn add-location
  "When entry defines a :location, adds node for location. Does nothing otherwise."
  [graph entry]
  (if (:location entry)
    (-> graph
        (uc/add-nodes :locations)
        (uc/add-edges [:locations (:timestamp entry)]))
    graph))

(defn add-briefing
  "When entry defines a :briefing, adds node. Does nothing otherwise."
  [graph entry]
  (if-let [briefing-day (-> entry :briefing :day)]
    (let [dt (ctf/parse (ctf/formatters :year-month-day) briefing-day)
          year (ct/year dt)
          month (ct/month dt)
          day (ct/day dt)
          day-node {:type :timeline/day :year year :month month :day day}]
      (-> graph
          (uc/add-nodes :briefings day-node)
          (uc/add-edges [day-node (:timestamp entry) {:relationship :BRIEFING}]
                        [:briefings (:timestamp entry)])))
    graph))

(defn add-story
  "When entry is a :story, adds node for story.
   Does nothing when entry is not of type :story."
  [graph entry]
  (if (= (:entry-type entry) :story)
    (-> graph
        (uc/add-nodes :stories)
        (uc/add-edges [:stories (:timestamp entry)]))
    graph))

(defn add-book
  "When entry is a :book, adds node for book.
   Does nothing when entry is not of type :book."
  [graph entry]
  (if (= (:entry-type entry) :book)
    (-> graph
        (uc/add-nodes :book)
        (uc/add-edges [:books (:timestamp entry)]))
    graph))

(defn add-story-set
  "When entry is linked to a story, add that entry timestamp to the set with
   the entry ids on that timeline."
  [current-state entry]
  (if-let [linked-story (:linked-story entry)]
    (let [ts (:timestamp entry)
          path [:sorted-story-entries linked-story]
          entries-set (into (sorted-set) (get-in current-state path))]
      (assoc-in current-state path (conj entries-set ts)))
    current-state))

(defn add-tasks-set
  "When entry is a task, add it to sorted tasks set."
  [current-state entry]
  (if (:task entry)
    (let [ts (:timestamp entry)
          path [:sorted-tasks]
          entries-set (into (sorted-set) (get-in current-state path))]
      (assoc-in current-state path (conj entries-set ts)))
    current-state))

(defn add-node
  "Adds node to both graph and the sorted set, which maintains the entries
   sorted by timestamp."
  [current-state ts entry startup?]
  (let [graph (:graph current-state)
        old-entry (when (uc/has-node? graph ts) (uc/attrs graph ts))
        merged (merge old-entry entry)
        old-tags (:tags old-entry)
        old-mentions (:mentions old-entry)
        remove-tag-edges (fn [g tags k]
                           (let [reducing-fn
                                 (fn [g ltag] (uc/remove-edges g [ts {k ltag}]))]
                             (reduce reducing-fn g (map s/lower-case tags))))
        media-tags (set (filter identity [(when (:img-file entry) "#photo")
                                          (when (:audio-file entry) "#audio")
                                          (when (:video entry) "#video")]))
        new-entry (update-in merged [:tags] #(set/union (set %) media-tags))]
    (-> current-state
        (update-in [:graph] remove-tag-edges old-tags :tag)
        (update-in [:graph] remove-tag-edges old-tags :ptag)
        (update-in [:graph] remove-tag-edges old-mentions :mention)
        (update-in [:graph] remove-unused-tags old-tags :tag)
        (update-in [:graph] remove-unused-tags old-mentions :mention)
        (update-in [:graph] uc/add-nodes-with-attrs [ts new-entry])
        (add-hashtags new-entry)
        (update-in [:graph] add-mentions new-entry)
        (update-in [:graph] add-linked new-entry)
        (add-daily-summary new-entry startup?)
        (add-timeline-tree new-entry)
        (update-in [:graph] add-linked-visit new-entry)
        (update-in [:graph] add-parent-ref new-entry)
        (update-in [:graph] add-story new-entry)
        (update-in [:graph] add-book new-entry)
        (update-in [:graph] add-location new-entry)
        (update-in [:graph] add-briefing new-entry)
        (add-story-set new-entry)
        (add-tasks-set new-entry)
        (update-in [:sorted-entries] conj ts))))

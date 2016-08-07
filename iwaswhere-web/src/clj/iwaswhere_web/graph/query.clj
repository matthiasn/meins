(ns iwaswhere-web.graph.query
  "this namespace manages interactions with the graph data structure, which
  holds all entries and their connections."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.fulltext-search :as ft]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]
            [clojure.string :as s]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clj-time.format :as timef]
            [clj-time.format :as ctf]
            [clojure.core.reducers :as r]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags and mentions in the
   query are contained in the filtered entry or any of it's comments, and none
    of the not-tags. Also allows filtering per day."
  [q graph]
  (fn [entry]
    (let [local-fmt (timef/with-zone (timef/formatters :year-month-day)
                                     (ct/default-time-zone))
          entry-day (timef/unparse local-fmt (ctc/from-long (:timestamp entry)))
          q-day (:date-string q)
          day-match? (= q-day entry-day)

          q-timestamp (:timestamp q)
          q-ts-match? (= q-timestamp (str (:timestamp entry)))

          q-tags (set (map s/lower-case (:tags q)))
          q-not-tags (set (map s/lower-case (:not-tags q)))
          q-mentions (set (map s/lower-case (:mentions q)))

          entry-tags (set (map s/lower-case (:tags entry)))
          entry-comments (map #(uber/attrs graph  %) (:comments entry))
          entry-comments-tags (apply set/union (map :tags entry-comments))
          tags (set (map s/lower-case (set/union entry-tags entry-comments-tags)))

          entry-mentions (set (map s/lower-case (:mentions entry)))
          entry-comments-mentions (apply set/union (map :mentions
                                                        (:comments entry)))
          mentions (set (map s/lower-case
                             (set/union entry-mentions entry-comments-mentions)))

          match? (and (set/subset? q-tags tags)
                      (empty? (set/intersection q-not-tags tags))
                      (or (empty? q-mentions)
                          (seq (set/intersection q-mentions mentions)))
                      (or day-match? (empty? q-day))
                      (or q-ts-match? (empty? q-timestamp)))]
      match?)))

(defn compare-w-upvotes
  "Sort comparator which considers upvotes first, and, if those are equal, the
   timestamp second."
  [x y]
  (let [upvotes-x (get x :upvotes 0)
        upvotes-y (get y :upvotes 0)]
    (if-not (= upvotes-x upvotes-y)
      (clojure.lang.Util/compare upvotes-y upvotes-x)
      (if (pos? upvotes-x)    ; when entries have upvotes, sort oldest on top
        (clojure.lang.Util/compare (:timestamp x) (:timestamp y))
        (clojure.lang.Util/compare (:timestamp y) (:timestamp x))))))

(defn get-comments
  "Extract all comments for entry."
  [entry g n]
  (merge entry
         {:comments (->> (flatten (uber/find-edges g {:dest n
                                                      :relationship :COMMENT}))
                         (remove :mirror?)
                         (map :src)
                         (sort))}))

(defn get-tags-mentions-matches
  "Extract matching timestamps for query."
  [g query]
  (let [mapper (fn [tag-type]
                 (fn [tag]
                   (set (map :dest
                             (uber/find-edges g {:src          {tag-type tag}
                                                 :relationship :CONTAINS})))))
        t-matched (map (mapper :tag) (map s/lower-case (:tags query)))
        m-matched (map (mapper :mention) (map s/lower-case (:mentions query)))]
    (apply set/union (concat t-matched m-matched))))

(defn get-nodes-for-day
  "Extract matching timestamps for query."
  [g query]
  (let [dt (ctf/parse (ctf/formatters :year-month-day) (:date-string query))]
    (set (map :dest (uber/find-edges g {:src          {:type  :timeline/day
                                                       :year  (ct/year dt)
                                                       :month (ct/month dt)
                                                       :day   (ct/day dt)}
                                        :relationship :DATE})))))

(defn get-linked-entries
  "Extract all linked entries for entry, including their comments."
  [entry g n sort-by-upvotes?]
  (let [linked (->> (flatten (uber/find-edges g {:src n :relationship :LINKED}))
                    ;(remove :mirror?)
                    (map :dest)
                    (sort))]
    (merge entry {:linked-entries-list (if sort-by-upvotes?
                                         (sort compare-w-upvotes linked)
                                         linked)})))

(defn extract-sorted-entries
  "Extracts nodes and their properties in descending timestamp order by looking
   for node by mapping over the sorted set and extracting attributes for each
   node.
   Warns when node not in graph. (debugging, should never happen)"
  [current-state query]
  (let [sort-by-upvotes? (:sort-by-upvotes query)
        g (:graph current-state)
        mapper-fn (fn [n]
                    (if (uber/has-node? g n)
                      (-> (uber/attrs g n)
                          (get-comments g n)
                          (get-linked-entries g n sort-by-upvotes?))
                      (log/warn "extract-sorted-entries can't find node: " n)))
        sort-fn #(into (sorted-set-by >) %)
        matched-entries (cond
                          ; set with timestamps matching tags and mentions
                          (or (seq (:tags query)) (seq (:mentions query)))
                          (sort-fn (get-tags-mentions-matches g query))
                          ; full-text search
                          (:ft-search query)
                          (sort-fn (ft/search query))
                          ; set with the one timestamp in query
                          (:timestamp query) #{(Long/parseLong
                                                 (:timestamp query))}
                          ; set with timestamps matching the day
                          (:date-string query) (sort-fn
                                                 (get-nodes-for-day g query))
                          ; set with all timestamps (leads to full scan)
                          :else (:sorted-entries current-state))
        entries (map mapper-fn matched-entries)]
    (if sort-by-upvotes?
      (sort compare-w-upvotes entries)
      entries)))

(defn extract-entries-by-ts
  "Find all entries for given timestamps set."
  [current-state entry-timestamps]
  (map (fn [n]
         (let [g (:graph current-state)]
           (if (uber/has-node? g n)
             (let [entry (uber/attrs g n)]
               (when (empty? entry) (log/warn "empty node:" entry))
               entry)
             (log/warn "extract-entries-by-ts can't find node: " n))))
       entry-timestamps))

(defn find-all-hashtags
  "Finds all hashtags used in entries by finding the edges that originate from
   the :hashtags node."
  [current-state]
  (let [g (:graph current-state)
        ltags (map #(-> % :dest :tag) (uber/find-edges g {:src :hashtags}))
        tags (map #(:val (uber/attrs g {:tag %})) ltags)]
    (set tags)))

(defn find-all-mentions
  "Finds all hashtags used in entries by finding the edges that originate from
   the :hashtags node."
  [current-state]
  (let [g (:graph current-state)
        lmentions (map #(-> % :dest :mention)
                       (uber/find-edges g {:src :mentions}))
        mentions (map #(:val (uber/attrs g {:mention %})) lmentions)]
    (set mentions)))

(defn get-filtered-results
  "Retrieve items to show in UI, also deliver all hashtags for autocomplete and
   some basic stats."
  [current-state query]
  (let [n (:n query)
        sort-by-upvotes? (:sort-by-upvotes query)
        graph (:graph current-state)
        entry-mapper (fn [entry] [(:timestamp entry) entry])
        entries (take n (filter (entries-filter-fn query graph)
                                (extract-sorted-entries current-state query)))
        comment-timestamps (set (flatten (map :comments entries)))
        linked-entries (extract-entries-by-ts current-state
                         (set (flatten (map :linked-entries-list entries))))
        linked-entries (map (fn [entry]
                              (let [ts (:timestamp entry)]
                                (-> entry
                                    (get-comments graph ts)
                                    (get-linked-entries graph ts sort-by-upvotes?))))
                            linked-entries)
        linked-comments-ts (set (flatten (map :comments linked-entries)))
        comments (extract-entries-by-ts current-state
                   (set/union comment-timestamps linked-comments-ts))]
    {:entries     (map :timestamp entries)
     :entries-map (merge (into {} (map entry-mapper entries))
                         (into {} (map entry-mapper comments))
                         (into {} (map entry-mapper linked-entries)))
     :hashtags    (:hashtags current-state)
     :mentions    (:mentions current-state)
     :stats       (:stats current-state)}))

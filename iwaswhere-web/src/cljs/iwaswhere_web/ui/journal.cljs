(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.ui.entry :as e]
            [iwaswhere-web.ui.pomodoro :as p]
            [iwaswhere-web.utils.parse :as ps]
            [clojure.set :as set]
            [cljs.pprint :as pp]))

(defn linked-entries-filter
  "Filter linked entries by search."
  [entries-map linked-filter]
  (fn [entry]
    (let [comments (map #(get entries-map %) (:comments entry))
          combined-tags (reduce #(set/union %1 (:tags %2)) (:tags entry) comments)]
      (and (set/subset? (:tags linked-filter) combined-tags)
           (empty? (set/intersection (:not-tags linked-filter) combined-tags))))))

(defn linked-entries-view
  "Renders linked entries in right side column, filtered by local search."
  [linked-entries entries-map new-entries cfg put-fn]
  (when linked-entries
    (let [on-input-fn #(let [linked-filter (ps/parse-search (.. % -target -innerText))]
                        (put-fn [:linked-filter/set linked-filter]))
          linked-entries (if (:show-pvt cfg)
                           linked-entries
                           (filter u/pvt-filter linked-entries))
          linked-entries (filter (linked-entries-filter entries-map (:linked-filter cfg))
                                 linked-entries)]
      [:div.journal-entries
       (when (:active cfg)
         [:div.search-field {:content-editable true :on-input on-input-fn}
          (:search-text (:linked-filter cfg))])
       (for [entry linked-entries]
         (when (and (not (:comment-for entry)) (or (:new-entry entry) (:show-context cfg)))
           (let [entry (assoc-in entry [:comments] (map (fn [ts] (get entries-map ts))
                                                        (:comments entry)))]
             ^{:key (:timestamp entry)}
             [e/entry-with-comments entry cfg new-entries put-fn])))])))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries store-snapshot))
        show-pvt? (:show-pvt cfg)
        filtered-entries (if show-pvt? entries (filter u/pvt-filter entries))
        new-entries (:new-entries store-snapshot)
        show-context? (:show-context cfg)
        comments-w-entries? (:comments-w-entries cfg)
        with-comments? (fn [entry] (and (or (and comments-w-entries? (not (:comment-for entry)))
                                            (not comments-w-entries?))
                                        (or (:new-entry entry) show-context?)))
        active-entry (get (:entries-map store-snapshot) (:active (:cfg store-snapshot)))
        linked-entries-set (set (:linked-entries-list active-entry))
        linked-entries (map (fn [ts] (get entries-map ts)) linked-entries-set)]
    [:div.journal
     [:div.journal-entries
      (for [entry (filter #(and (not (:comment-for %))
                                (not (contains? (:entries-map store-snapshot) (:timestamp %)))
                                (not (contains? linked-entries-set (:timestamp %))))
                          (vals new-entries))]
        ^{:key (:timestamp entry)}
        [e/entry-with-comments entry cfg new-entries put-fn])
      (for [entry (filter #(not (contains? linked-entries-set (:timestamp %))) filtered-entries)]
        (when (with-comments? entry)
          (let [entry (assoc-in entry [:comments] (map (fn [ts] (get entries-map ts))
                                                       (:comments entry)))]
            ^{:key (:timestamp entry)}
            [e/entry-with-comments entry cfg new-entries put-fn])))
      (when (and show-context? (seq entries))
        (let [show-more #(put-fn [:show/more])]
          [:div.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats)
         " edges, " (count (:hashtags cfg)) " hashtags, " (count (:mentions cfg)) " people"])
      [:div (p/pomodoro-stats-str filtered-entries)]
      (when-let [ms (get-in store-snapshot [:timing :query])]
        [:div.stats (str "Query with " (count entries) " results completed in " ms ", RTT "
                         (get-in store-snapshot [:timing :rtt]) " ms")])]
     (linked-entries-view linked-entries entries-map new-entries cfg put-fn)]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

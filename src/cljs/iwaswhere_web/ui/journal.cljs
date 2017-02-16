(ns iwaswhere-web.ui.journal
  (:require [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.ui.entry.entry :as e]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.utils.parse :as ps]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.set :as set]))

(defn linked-filter-fn
  "Filter linked entries by search."
  [entries-map linked-filter put-fn]
  (fn [entry]
    (let [comments-mapper (u/find-missing-entry entries-map put-fn)
          comments (mapv comments-mapper (:comments entry))
          combined-tags (reduce #(set/union %1 (:tags %2)) (:tags entry)
                                comments)]
      (and (set/subset? (:tags linked-filter) combined-tags)
           (empty? (set/intersection (:not-tags linked-filter)
                                     combined-tags))))))

(defn linked-entries-view
  "Renders linked entries in right side column, filtered by local search."
  [put-fn local-cfg active-entry]
  (let [cfg (subscribe [:cfg])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])]
    (fn linked-entries-render [put-fn local-cfg active-entry]
      (let [conf (merge @cfg @options)
            linked-entries-set (into (sorted-set) (:linked-entries-list active-entry))
            linked-mapper (u/find-missing-entry @entries-map put-fn)
            linked-entries (map linked-mapper linked-entries-set)
            query-id (:query-id local-cfg)
            on-input-fn #(put-fn [:linked-filter/set
                                  {:search   (ps/parse-search
                                               (.. % -target -innerText))
                                   :query-id query-id}])
            linked-entries (if (:show-pvt conf)
                             linked-entries
                             (filter (u/pvt-filter conf @entries-map) linked-entries))
            linked-filter (query-id (:linked-filter conf))
            filter-fn (linked-filter-fn @entries-map linked-filter put-fn)
            linked-entries (filter filter-fn linked-entries)]
        (when linked-entries
          [:div.journal-entries
           (when (query-id (:active conf))
             [:div.linked-search-field
              {:content-editable true :on-input on-input-fn}
              (:search-text (query-id (:linked-filter conf)))])
           (for [entry linked-entries]
             (when (and (not (:comment-for entry))
                        (or (:new-entry entry) (:show-context conf)))
               ^{:key (str "linked-" (:timestamp entry))}
               [e/entry-with-comments entry put-fn local-cfg]))])))))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the
   entry."
  [local-cfg put-fn]
  (let [cfg (subscribe [:cfg])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])
        results (subscribe [:results])
        active (reaction (-> @cfg :active))]
    (fn journal-view-render [local-cfg put-fn]
      (let [conf (merge @cfg @options)
            query-id (:query-id local-cfg)
            active-id (query-id @active)
            active-entry (get @entries-map active-id)
            entries (query-id @results)
            entries-map-deref @entries-map
            entries (map (fn [ts] (get entries-map-deref ts)) entries)
            show-pvt? (:show-pvt conf)
            filtered-entries (if show-pvt?
                               entries
                               (filter (u/pvt-filter conf @entries-map) entries))
            show-context? (:show-context conf)
            comments-w-entries? (not (:comments-standalone conf))
            with-comments? (fn [entry] (and (or (and comments-w-entries?
                                                     (not (:comment-for entry)))
                                                (not comments-w-entries?))
                                            (or (:new-entry entry) show-context?)))
            linked-entries-set (into (sorted-set) (:linked-entries-list active-entry))]
        [:div.journal
         [:div.journal-entries
          (for [entry (filter #(not (contains? linked-entries-set (:timestamp %)))
                              filtered-entries)]
            (when (with-comments? entry)
              ^{:key (:timestamp entry)}
              [e/entry-with-comments entry put-fn local-cfg]))
          (when (seq entries)
            (let [show-more #(put-fn [:show/more {:query-id query-id}])]
              [:div.show-more {:on-click show-more :on-mouse-over show-more}
               [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))]
         (when active-entry
           [linked-entries-view put-fn local-cfg active-entry])]))))

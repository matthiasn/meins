(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.ui.entry :as e]
            [iwaswhere-web.ui.search :as search]
            [iwaswhere-web.utils.parse :as ps]
            [clojure.set :as set]))

(defn find-missing-entry
  [entries-map put-fn]
  (fn [ts]
    (let [entry (get entries-map ts)]
      (or entry
          (let [missing-entry {:timestamp ts}]
            (put-fn [:entry/find missing-entry])
            missing-entry)))))

(defn linked-filter-fn
  "Filter linked entries by search."
  [entries-map linked-filter put-fn]
  (fn [entry]
    (let [comments-mapper (find-missing-entry entries-map put-fn)
          comments (map comments-mapper (:comments entry))
          combined-tags (reduce #(set/union %1 (:tags %2)) (:tags entry)
                                comments)]
      (and (set/subset? (:tags linked-filter) combined-tags)
           (empty? (set/intersection (:not-tags linked-filter)
                                     combined-tags))))))

(defn linked-entries-view
  "Renders linked entries in right side column, filtered by local search."
  [linked-entries entries-map new-entries cfg put-fn local-cfg]
  (when linked-entries
    (let [query-id (:query-id local-cfg)
          on-input-fn #(put-fn [:linked-filter/set
                                {:search (ps/parse-search
                                           (.. % -target -innerText))
                                 :query-id query-id}])
          linked-entries (if (:show-pvt cfg)
                           linked-entries
                           (filter (u/pvt-filter cfg) linked-entries))
          linked-filter (query-id (:linked-filter cfg))
          filter-fn (linked-filter-fn entries-map linked-filter put-fn)
          linked-entries (filter filter-fn linked-entries)]
      [:div.journal-entries
       (when (query-id (:active cfg))
         [:div.search-field {:content-editable true :on-input on-input-fn}
          (:search-text (query-id (:linked-filter cfg)))])
       (for [entry linked-entries]
         (when (and (not (:comment-for entry))
                    (or (:new-entry entry) (:show-context cfg)))
           (let [entry (assoc-in entry [:comments]
                                 (map (fn [ts] (get entries-map ts))
                                      (:comments entry)))]
             ^{:key (str "linked-" (:timestamp entry))}
             [e/entry-with-comments
              entry cfg new-entries put-fn entries-map local-cfg])))])))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the
   entry."
  [{:keys [observed put-fn]} query-id]
  (let [store-snapshot @observed
        cfg (:cfg store-snapshot)
        local-cfg {:query-id query-id}
        results (query-id (:results store-snapshot))
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries results))
        show-pvt? (:show-pvt cfg)
        filtered-entries (if show-pvt?
                           entries
                           (filter (u/pvt-filter cfg) entries))
        new-entries (:new-entries store-snapshot)
        show-context? (:show-context cfg)
        comments-w-entries? (:comments-w-entries cfg)
        with-comments? (fn [entry] (and (or (and comments-w-entries?
                                                 (not (:comment-for entry)))
                                            (not comments-w-entries?))
                                        (or (:new-entry entry) show-context?)))
        active-id (-> store-snapshot :cfg :active query-id)
        active-entry (get entries-map active-id)
        linked-entries-set (set (:linked-entries-list active-entry))
        linked-mapper (find-missing-entry entries-map put-fn)
        linked-entries (map linked-mapper linked-entries-set)]
    [:div.journal
     [:div.journal-entries
      (for [entry (filter #(not (contains? linked-entries-set (:timestamp %)))
                          filtered-entries)]
        (when (with-comments? entry)
          (let [entry (assoc-in entry [:comments]
                                (map (fn [ts] (get entries-map ts))
                                     (:comments entry)))]
            ^{:key (:timestamp entry)}
            [e/entry-with-comments
             entry cfg new-entries put-fn entries-map local-cfg])))
      (when (seq entries)
        (let [show-more #(put-fn [:show/more {:query-id query-id}])]
          [:div.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))]
     (when active-entry
       [linked-entries-view
        linked-entries entries-map new-entries cfg put-fn local-cfg])]))

(defn tab-view
  [{:keys [observed put-fn] :as cmp-map} query-id tabs-group]
  (let [store-snapshot @observed]
    [:div.tab-view
     [search/search-field-view store-snapshot put-fn query-id]
     [journal-view cmp-map query-id]]))

(defn new-entries-view
  "Renders view for creating new entries at the top of the page."
  [store-snapshot local-cfg put-fn]
  (let [cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        new-entries (:new-entries store-snapshot)]
    [:div.new-entries
     (for [entry (filter #(and
                           (not (:comment-for %))
                           (not (contains? entries-map (:timestamp %))))
                         (vals new-entries))]
       ^{:key (:timestamp entry)}
       [e/entry-with-comments entry
        cfg new-entries put-fn entries-map local-cfg])]))

(defn tabs-view
  "Renders a split view, with new entries at the top."
  [{:keys [observed put-fn] :as cmp-map}]
  (let [store-snapshot @observed
        local-cfg {}]
    [:div.tabs-container
     [new-entries-view store-snapshot local-cfg put-fn]
     [:div.tabs-view
      [tab-view cmp-map :query-1 :left]
      [tab-view cmp-map :query-2 :right]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn tabs-view
              :dom-id  "journal"}))

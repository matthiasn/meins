(ns iwaswhere-web.ui.journal
  (:require [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.ui.entry.entry :as e]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.utils.parse :as ps]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.set :as set]
            [iwaswhere-web.ui.draft :as draft]))

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
            linked-entries (mapv linked-mapper linked-entries-set)
            query-id (:query-id local-cfg)
            linked-entries (if (:show-pvt conf)
                             linked-entries
                             (filter (u/pvt-filter conf @entries-map) linked-entries))
            linked-filter (query-id (:linked-filter conf))
            filter-fn (u/linked-filter-fn @entries-map linked-filter put-fn)
            linked-entries (filter filter-fn linked-entries)
            hashtags (:hashtags @options)
            pvt-hashtags (:pvt-hashtags @options)
            show-pvt? (:show-pvt @cfg)
            hashtags (if show-pvt? (concat hashtags pvt-hashtags) hashtags)
            mentions (:mentions @options)
            mentions-list (map (fn [m] {:name m}) mentions)
            hashtags-list (map (fn [h] {:name h}) hashtags)
            search-send (fn [plain state]
                          (let [s (merge (ps/parse-search plain)
                                         {:editor-state state})
                                filter {:search s :query-id query-id}]
                            (put-fn [:linked-filter/set filter])))
            search-editor-state (if-let [editor-state (:editor-state linked-filter)]
                                  (draft/editor-state-from-raw (clj->js editor-state))
                                  (draft/editor-state-from-text ""))]
        (when linked-entries
          [:div.journal-entries
           (when (query-id (:active conf))
             [draft/draft-search-field
              search-editor-state search-send mentions-list hashtags-list])
           (for [entry linked-entries]
             (when (and (not (:comment-for entry))
                        (or (:new-entry entry) (:show-context conf)))
               ^{:key (str "linked-" (:timestamp entry))}
               [e/entry-with-comments (:timestamp entry) put-fn local-cfg]))])))))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the
   entry."
  [local-cfg put-fn]
  (let [cfg (subscribe [:cfg])
        query-cfg (subscribe [:query-cfg])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])
        results (subscribe [:results])
        active (reaction (-> @cfg :active))]
    (fn journal-view-render [local-cfg put-fn]
      (let [conf (merge @cfg @options)
            query-id (:query-id local-cfg)
            query (reaction (get-in @query-cfg [:queries (:query-id local-cfg)]))
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
            linked-entries-set (into (sorted-set) (:linked-entries-list active-entry))
            find-missing (u/find-missing-entry @entries-map put-fn)]
        [:div.journal
         [:div.journal-entries
          (when-let [story (:story local-cfg)]
            (find-missing story)
            ^{:key story}
            [e/entry-with-comments story put-fn local-cfg])
          (for [entry (filter #(not (contains? linked-entries-set (:timestamp %)))
                              filtered-entries)]
            (when (with-comments? entry)
              ^{:key (:timestamp entry)}
              [e/entry-with-comments (:timestamp entry) put-fn local-cfg]))
          (when (> (count entries) 1)
            (let [show-more #(put-fn [:show/more {:query-id query-id}])]
              [:div.show-more {:on-click show-more :on-mouse-over show-more}
               [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))]
         (when active-entry
           [linked-entries-view put-fn local-cfg active-entry])]))))

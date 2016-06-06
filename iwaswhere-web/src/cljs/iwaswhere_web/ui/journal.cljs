(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.ui.entry :as e]
            [iwaswhere-web.ui.pomodoro :as p]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        cfg (:cfg store-snapshot)
        entries (:entries store-snapshot)
        show-pvt? (:show-pvt cfg)
        filtered-entries (if show-pvt? entries (filter u/pvt-filter entries))
        new-entries (:new-entries store-snapshot)
        show-context? (:show-context cfg)
        comments-w-entries? (:comments-w-entries cfg)
        with-comments? (fn [entry] (and (or (and comments-w-entries? (not (:comment-for entry)))
                                            (not comments-w-entries?))
                                        (or (:new-entry entry) show-context?)))
        active-entry (get (:entries-map store-snapshot) (:active (:cfg store-snapshot)))]
    [:div.journal
     [:div.journal-entries
      (for [entry (filter #(and (not (:comment-for %))
                                (not (contains? (:entries-map store-snapshot) (:timestamp %))))
                          (vals new-entries))]
        ^{:key (:timestamp entry)}
        [e/entry-with-comments entry cfg new-entries put-fn])
      (for [entry filtered-entries]
        (when (with-comments? entry)
          ^{:key (:timestamp entry)}
          [e/entry-with-comments entry cfg new-entries put-fn]))
      (when (and show-context? (seq entries))
        (let [show-more #(put-fn [:show/more {}])]
          [:div.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats) " edges, "
         (count (:hashtags cfg)) " hashtags, " (count (:mentions cfg)) " people"])
      [:div (p/pomodoro-stats-str filtered-entries)]
      (when-let [ms (:duration-ms store-snapshot)]
        [:div.stats (str "Query completed in " ms "ms")])]
     (when-let [linked-entries (:linked-entries-list active-entry)]
       [:div.journal-entries
        (for [entry (if show-pvt? linked-entries (filter u/pvt-filter linked-entries))]
          (when (and (not (:comment-for entry)) (or (:new-entry entry) show-context?))
            ^{:key (:timestamp entry)}
            [e/entry-with-comments entry cfg new-entries put-fn]))])]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.ui.entry :as e]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        cfg (:cfg store-snapshot)
        show-entries (or (:show-entries store-snapshot) 20)
        entries (take show-entries (:entries store-snapshot))
        new-entries (vals (:new-entries store-snapshot))
        show-context? (:show-context cfg)
        show-pvt? (:show-pvt cfg)
        active-entry (get (:entries-map store-snapshot) (:active store-snapshot))]
    [:div.journal
     [:div.journal-entries
      (for [entry (filter #(not (:comment-for %)) new-entries)]
        ^{:key (:timestamp entry)}
        [e/entry-with-comments entry cfg new-entries put-fn])
      (for [entry (if show-pvt? entries (filter u/pvt-filter entries))]
        (when (and (not (:comment-for entry)) (or (:new-entry entry) show-context?))
          ^{:key (:timestamp entry)}
          [e/entry-with-comments entry cfg new-entries put-fn]))
      (when (and show-context? (seq entries))
        (let [show-more #(put-fn [:show/more {}])]
          [:div.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats) " edges, "
         (count (:hashtags store-snapshot)) " hashtags, " (count (:mentions store-snapshot)) " people"])
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

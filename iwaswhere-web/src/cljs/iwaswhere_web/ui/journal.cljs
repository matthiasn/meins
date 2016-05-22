(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.ui.entry :as e]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed put-fn]}]
  (let [store-snapshot @observed
        show-entries (or (:show-entries store-snapshot) 20)
        entries (take show-entries (:entries store-snapshot))
        new-entries (vals (:new-entries store-snapshot))
        show-context? (:show-context store-snapshot)
        show-pvt? (:show-pvt store-snapshot)
        active-entry (get (:entries-map store-snapshot) (:active store-snapshot))]
    [:div.l-box-lrg.pure-g.journal
     {:style {:margin-top (.-offsetHeight (.-firstChild (.getElementById js/document "search")))}}
     [:div.journal-entries
      {:class (if active-entry "pure-u-1-2" "pure-u-1")}
      (for [entry (filter #(not (:comment-for %)) new-entries)]
        ^{:key (:timestamp entry)}
        [e/entry-with-comments entry store-snapshot put-fn true])
      (for [entry (if show-pvt? entries (filter u/pvt-filter entries))]
        (let [editable? (contains? (:tags entry) "#new-entry")]
          (when (and (not (:comment-for entry)) (or editable? show-context?))
            ^{:key (:timestamp entry)}
            [e/entry-with-comments entry store-snapshot put-fn false])))
      (when (and show-context? (seq entries))
        (let [show-more #(put-fn [:show/more {}])]
          [:div.pure-u-1.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div.pure-u-1 (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats) " edges, "
         (count (:hashtags store-snapshot)) " hashtags, " (count (:mentions store-snapshot)) " people"])
      (when-let [ms (:duration-ms store-snapshot)]
        [:div.pure-u-1 (str "Query completed in " ms "ms")])]
     (when-let [linked-entries (:linked-entries-list active-entry)]
       [:div.linked-entries
        {:class (if active-entry "pure-u-1-2" "pure-u-1")}
        (for [entry (if show-pvt? linked-entries (filter u/pvt-filter linked-entries))]
          (let [editable? (contains? (:tags entry) "#new-entry")]
            (when (and (not (:comment-for entry)) (or editable? show-context?))
              ^{:key (:timestamp entry)}
              [e/entry-with-comments entry store-snapshot put-fn false])))])]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

(ns iwaswhere-web.ui.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.utils :as u]
            [iwaswhere-web.ui.entry :as e]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        store-snapshot @observed
        show-entries (:show-entries local-snapshot)
        entries (take show-entries (:entries store-snapshot))
        new-entries (vals (:new-entries store-snapshot))
        hashtags (:hashtags store-snapshot)
        mentions (:mentions store-snapshot)
        show-all-maps? (:show-all-maps local-snapshot)
        toggle-all-maps #(swap! local update-in [:show-all-maps] not)
        show-tags? (:show-hashtags local-snapshot)
        toggle-tags #(swap! local update-in [:show-hashtags] not)
        show-context? (:show-context local-snapshot)
        toggle-context #(swap! local update-in [:show-context] not)
        show-pvt? (:show-pvt local-snapshot)
        toggle-pvt #(swap! local update-in [:show-pvt] not)
        show-comments? (:show-comments local-snapshot)
        toggle-comments #(swap! local update-in [:show-comments] not)]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:span.fa.fa-comments.toggle-map.pull-right
       {:class (when-not show-comments? "hidden-comments") :on-click toggle-comments}]
      [:span.fa.toggle-map.pull-right {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}]
      [:span.fa.fa-hashtag.toggle-map.pull-right {:class (when-not show-tags? "inactive") :on-click toggle-tags}]
      [:span.fa.fa-eye.toggle-map.pull-right {:class (when-not show-context? "inactive") :on-click toggle-context}]
      [:span.fa.fa-user-secret.toggle-map.pull-right {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}]
      [:hr]
      (for [entry (filter #(not (:comment-for %)) new-entries)]
        ^{:key (:timestamp entry)}
        [e/entry-with-comments
         entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-comments? true])
      (for [entry (if show-pvt? entries (filter u/pvt-filter entries))]
        (let [editable? (contains? (:tags entry) "#new-entry")]
          (when (and (not (:comment-for entry)) (or editable? show-context?))
            ^{:key (:timestamp entry)}
            [e/entry-with-comments
             entry store-snapshot hashtags mentions put-fn show-all-maps? show-tags? show-pvt? show-comments? false])))
      (when (and show-context? (seq entries))
        (let [show-more #(swap! local update-in [:show-entries] + 20)]
          [:div.pure-u-1.show-more {:on-click show-more :on-mouse-over show-more}
           [:span.show-more-btn [:span.fa.fa-plus-square] " show more"]]))
      (when-let [stats (:stats store-snapshot)]
        [:div.pure-u-1 (:entry-count stats) " entries, " (:node-count stats) " nodes, " (:edge-count stats) " edges, "
         (count hashtags) " hashtags, " (count mentions) " people"])
      (when-let [ms (:duration-ms store-snapshot)]
        [:div.pure-u-1 (str "Query completed in " ms "ms")])]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id        cmp-id
              :initial-state {:show-entries  20
                              :show-all-maps false
                              :show-hashtags true
                              :show-context  true
                              :show-pvt      false
                              :show-comments true}
              :view-fn       journal-view
              :dom-id        "journal"}))

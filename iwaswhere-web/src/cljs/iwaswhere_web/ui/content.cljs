(ns iwaswhere-web.ui.content
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.new-entries :as n]
            [iwaswhere-web.ui.search :as search]
            [iwaswhere-web.ui.journal :as j]
            [clojure.string :as s]))

(defn tabs-header-view
  [query-cfg tab-group put-fn]
  (let [queries (-> query-cfg :tab-groups tab-group :all)
        active-query (-> query-cfg :tab-groups tab-group :active)]
    [:div.tabs-header
     (for [q queries]
       (let [search-text (s/trim (str (get-in query-cfg [:queries q :search-text])))
             search-text (if (empty? search-text) "empty" search-text)
             query-coord {:query-id q :tab-group tab-group}]
         ^{:key (str "tab-header" q)}
         [:div.tab-item
          {:class    (when (= active-query q) "active")
           :on-click #(put-fn [:search/set-active query-coord])}
          [:span (str (or search-text q))
           [:span.fa.fa-times
            {:on-click #(do (put-fn [:search/remove query-coord])
                            (.stopPropagation %))}]]]))
     [:div.tab-item {:on-click #(put-fn [:search/add {:tab-group tab-group}])}
      [:span "add"]]]))

(defn split-window-view
  [{:keys [observed put-fn] :as cmp-map} tab-group]
  (let [snapshot @observed
        cfg (:cfg snapshot)
        query-cfg (:query-cfg snapshot)
        query-id (-> query-cfg :tab-groups tab-group :active)
        local-cfg {:query-id query-id :tab-group tab-group}]
    [:div.split-window-view {:class (when (:split-view cfg) "split-view")}
     [tabs-header-view query-cfg tab-group put-fn]
     (when query-id
       [search/search-field-view snapshot put-fn query-id])
     (when query-id
       [j/journal-view cmp-map local-cfg])]))

(defn split-windows-view
  "Renders a split view, with new entries at the top."
  [{:keys [observed put-fn] :as cmp-map}]
  (let [store-snapshot @observed
        local-cfg {}
        cfg (:cfg store-snapshot)]
    [:div.split-window-container
     [n/new-entries-view store-snapshot local-cfg put-fn]
     [:div.split-windows-view
      [split-window-view cmp-map :left]
      (when (:split-view cfg)
        [split-window-view cmp-map :right])]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn split-windows-view
              :dom-id  "content"}))

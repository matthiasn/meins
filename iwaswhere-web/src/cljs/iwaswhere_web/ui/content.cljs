(ns iwaswhere-web.ui.content
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.new-entries :as n]
            [iwaswhere-web.ui.search :as search]
            [iwaswhere-web.ui.journal :as j]
            [clojure.string :as s]
            [iwaswhere-web.ui.stats :as stats]
            [iwaswhere-web.helpers :as h]
            [cljsjs.react-grid-layout]
            [reagent.core :as rc]
            [cljs.pprint :as pp]
            [iwaswhere-web.ui.charts.custom-fields :as cf]))

(defn tabs-header-view
  [query-cfg tab-group put-fn]
  (let [queries (-> query-cfg :tab-groups tab-group :all)
        active-query (-> query-cfg :tab-groups tab-group :active)
        on-drop #(let [dragged (:dragged query-cfg)]
                   (when (not= tab-group (:tab-group dragged))
                     (put-fn [:search/move-tab {:dragged dragged :to tab-group}]))
                   (.preventDefault %))]
    [:div.tabs-header {:on-drop       on-drop
                       :on-drag-over  h/prevent-default
                       :on-drag-enter h/prevent-default}
     (for [q queries]
       (let [search-text (s/trim (str (get-in query-cfg [:queries q :search-text])))
             search-text (if (empty? search-text) "empty" search-text)
             query-coord {:query-id q :tab-group tab-group}
             on-drag-start #(put-fn [:search/set-dragged query-coord])]
         ^{:key (str "tab-header" q)}
         [:div.tab-item
          {:class         (when (= active-query q) "active")
           :on-click      #(put-fn [:search/set-active query-coord])
           :draggable     true
           :on-drag-start on-drag-start}
          [:span (str (or search-text q))
           [:span.fa.fa-times
            {:on-click #(do (put-fn [:search/remove query-coord])
                            (.stopPropagation %))}]]]))
     [:div.tab-item {:on-click #(put-fn [:search/add {:tab-group tab-group}])}
      [:span "add"]]]))

(defn tabs-view
  [{:keys [observed put-fn] :as cmp-map} tab-group]
  (let [snapshot @observed
        query-cfg (:query-cfg snapshot)
        query-id (-> query-cfg :tab-groups tab-group :active)
        local-cfg {:query-id query-id :tab-group tab-group}]
    [:div.tile-tabs
     [tabs-header-view query-cfg tab-group put-fn]
     (when query-id
       [search/search-field-view snapshot put-fn query-id])
     (when query-id
       [j/journal-view cmp-map local-cfg])]))

(defn widget-view
  [{:keys [observed put-fn] :as cmp-map} id cfg]
  (let [t (:type cfg)
        snapshot @observed]
    [:div.widget {:key       id
                  :data-grid (:data-grid cfg)}
     (case t
       :tabs-view [tabs-view cmp-map (:query-id cfg)]
       :custom-fields-chart (let [stats (:custom-field-stats snapshot)
                                  options (:options snapshot)]
                              [cf/custom-fields-chart stats put-fn options])
       :all-stats-chart [stats/stats-view cmp-map]
       [:div "unknown type"])]))

(def react-grid-layout (rc/adapt-react-class js/ReactGridLayout))

(defn grid-view
  "Renders grid view."
  [{:keys [observed put-fn] :as cmp-map}]
  (let [snapshot @observed
        local-cfg {}
        cfg (:cfg snapshot)
        configurable? (:reconfigure-grid cfg)
        widgets (:widgets cfg)]
    [:div.grid-view
     (when (seq widgets)
       (into
         [react-grid-layout
          {:width            (.-innerWidth js/window)
           :row-height       20
           :cols             24
           :margin           [8 8]
           :is-draggable     configurable?
           :is-resizable     configurable?
           :class            "tile-journal"
           :on-layout-change (fn [layout]
                               (let [new (js->clj layout :keywordize-keys true)]
                                 (put-fn [:layout/save new])))}]
         (mapv (fn [[k v]] (widget-view cmp-map k v)) widgets)))
     [n/new-entries-view snapshot local-cfg put-fn]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn grid-view
              :dom-id  "content"}))

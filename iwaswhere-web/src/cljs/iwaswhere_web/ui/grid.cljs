(ns iwaswhere-web.ui.grid
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.custom-fields :as cf2]
            [cljs.pprint :as pp]
            [iwaswhere-web.ui.journal :as j]
            [clojure.string :as s]
            [cljsjs.react-grid-layout]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.search :as search]
            [iwaswhere-web.ui.stats :as stats]
            [re-frame.core :refer [reg-event-db path reg-sub dispatch
                                   dispatch-sync subscribe]]))

(defn tabs-header-view
  [tab-group put-fn]
  (let [query-cfg (subscribe [:query-cfg])]
    (fn tabs-header-view2-render
      [tab-group put-fn]
      (let [query-config @query-cfg
            queries (-> query-config :tab-groups tab-group :all)
            active-query (-> query-config :tab-groups tab-group :active)
            on-drop #(let [dragged (:dragged query-config)]
                       (when (not= tab-group (:tab-group dragged))
                         (put-fn [:search/move-tab {:dragged dragged :to tab-group}]))
                       (.preventDefault %))]
        [:div.tabs-header {:on-drop       on-drop
                           :on-drag-over  h/prevent-default
                           :on-drag-enter h/prevent-default}
         (for [q queries]
           (let [search-text (s/trim (str (get-in query-config [:queries q :search-text])))
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
          [:span "add"]]]))))

(defn tabs-view
  [tab-group put-fn]
  (let [query-cfg (subscribe [:query-cfg])]
    (fn tabs-render [tab-group put-fn]
      (let [query-id (-> @query-cfg :tab-groups tab-group :active)
            local-cfg {:query-id query-id :tab-group tab-group}]
        [:div.tile-tabs
         [tabs-header-view tab-group put-fn]
         (when query-id
           [search/search-field-view2 query-id put-fn])
         (when query-id
           [j/journal-view local-cfg put-fn])]))))

(def react-grid-layout (rc/adapt-react-class js/ReactGridLayout))

(defn widget-view
  [id widget-cfg put-fn]
  (let [t (:type widget-cfg)]
    [:div.widget {:key       id
                  :data-grid (:data-grid widget-cfg)}
     (case t
       :tabs-view [tabs-view (:query-id widget-cfg) put-fn]
       :custom-fields-chart [cf2/custom-fields-chart put-fn]
       :all-stats-chart [stats/stats-view put-fn]
       [:div "unknown type"])]))

(defn grid
  [put-fn]
  (let [cfg (subscribe [:cfg])
        widgets (subscribe [:widgets])]
    (fn grid-render
      [put-fn]
      (let [configurable? (:reconfigure-grid @cfg)]
        [:div.grid-view
         (when (seq @widgets)
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
             (mapv (fn [[k v]] (widget-view k v put-fn)) @widgets)))]))))

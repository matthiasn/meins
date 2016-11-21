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

(def react-grid-layout (rc/adapt-react-class js/ReactGridLayout))

(defn grid-view
  "Renders grid view."
  [{:keys [observed put-fn] :as cmp-map}]
  (let [snapshot @observed
        local-cfg {}
        cfg (:cfg snapshot)
        configurable? (:reconfigure-grid cfg)
        dom-node (rc/dom-node (rc/current-component))
        w (if dom-node (.-offsetWidth dom-node) 1200)]
    [:div.grid-view
     [react-grid-layout
      {:width            w
       :row-height       20
       :cols             24
       :margin           [8 8]
       :is-draggable     configurable?
       :is-resizable     configurable?
       :class            "tile-journal"
       :on-layout-change (fn [layout]
                           (pp/pprint (js->clj layout :keywordize-keys true)))}
      [:div.widget {:key       :custom-fields
                    :data-grid {:x 0 :y 0 :w 6 :h 16}}
       [:div.stats
        [cf/custom-fields-chart
         (:custom-field-stats snapshot) put-fn (:options snapshot)]]]
      [:div.widget {:key       :all-stats
                    :data-grid {:x 0 :y 0 :w 6 :h 16}}
       [stats/stats-view cmp-map]]
      [:div.widget {:key       :split-left
                    :data-grid {:x 6 :y 0 :w 9 :h 19}}
       [tabs-view cmp-map :left]]
      [:div.widget {:key       :split-right
                    :data-grid {:x 15 :y 0 :w 9 :h 19}}
       [tabs-view cmp-map :right]]
      #_[:div.widget {:key       :split-right2
                      :data-grid {:x 15 :y 17 :w 9 :h 16}}
         [tabs-view cmp-map :right2]]]
     [n/new-entries-view snapshot local-cfg put-fn]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn grid-view
              :dom-id  "content"}))

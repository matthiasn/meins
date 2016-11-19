(ns iwaswhere-web.ui.content
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.ui.new-entries :as n]
            [iwaswhere-web.ui.search :as search]
            [iwaswhere-web.ui.journal :as j]
            [clojure.string :as s]
            [iwaswhere-web.ui.stats :as stats]
            [iwaswhere-web.helpers :as h]
            [cljsjs.react-grid-layout]
            [iwaswhere-web.utils.parse :as p]
            [reagent.core :as rc]
            [iwaswhere-web.ui.charts.pomodoros :as cp]
            [iwaswhere-web.ui.charts.daily-summaries :as ds]
            [iwaswhere-web.ui.charts.wordcount :as wc]))

(defn tabs-header-view
  [query-cfg tab-group put-fn]
  (let [queries (-> query-cfg :tab-groups tab-group :all)
        active-query (-> query-cfg :tab-groups tab-group :active)
        on-drop #(let [dragged (:dragged query-cfg)
                       dragged-id (:query-id dragged)
                       dragged-query (dragged-id (:queries query-cfg))]
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

(defn split-window-view
  [{:keys [observed put-fn] :as cmp-map} tab-group]
  (let [snapshot @observed
        cfg (:cfg snapshot)
        query-cfg (:query-cfg snapshot)
        query-id (-> query-cfg :tab-groups tab-group :active)
        local-cfg {:query-id query-id :tab-group tab-group}]
    [:div.split-window-view
     [tabs-header-view query-cfg tab-group put-fn]
     (when query-id
       [search/search-field-view snapshot put-fn query-id])
     (when query-id
       [j/journal-view cmp-map local-cfg])]))

(defn split-window-view2
  [{:keys [observed put-fn] :as cmp-map} tab-group]
  (let [snapshot @observed
        cfg (:cfg snapshot)
        query-cfg (:query-cfg snapshot)
        query-id (-> query-cfg :tab-groups tab-group :active)
        local-cfg {:query-id query-id :tab-group tab-group}]
    [:div.tile-tabs
     [tabs-header-view query-cfg tab-group put-fn]
     (when query-id
       [search/search-field-view snapshot put-fn query-id])
     (when query-id
       [j/journal-view cmp-map local-cfg])]))

(defn GridItem
  [props data]
  (prn props data)
  ^{:key (:i data)}
  [:div "test"])

(defn onLayoutChange [on-change prev new]
  ;; note the need to convert the callbacks from js objects
  (on-change prev (js->clj new :keywordize-keys true)))

(defn Grid
  [args]
  (rc/create-class
    ;; probably dont need this..
    {:should-component-update
     (fn [this [_ old-props] [_ new-props]]
       ;; just re-render when data changes and width changes
       (or (not= (:width old-props) (:width new-props))
           (not= (:data old-props) (:data new-props))))
     :reagent-render
     (fn [{:keys [id data width row-height cols item-props on-change empty-text] :as props}]
       [:div.grid-container.split-window-view
        (if (seq data)
          (into [:> js/ReactGridLayout {:id              id
                                        :cols            cols
                                        :initial-width   width
                                        :row-height      row-height
                                        :draggableHandle ".grid-toolbar"
                                        :draggableCancel ".grid-content"
                                        ;:onLayoutChange  (partial onLayoutChange on-change data)
                                        }]
                (mapv (partial GridItem item-props) data)))])}))

(def rgl (rc/adapt-react-class js/ReactGridLayout))

(defn split-windows-view
  "Renders a split view, with new entries at the top."
  [{:keys [observed put-fn] :as cmp-map}]
  (let [store-snapshot @observed
        local-cfg {}
        cfg (:cfg store-snapshot)]
    [:div.split-window-container

     [:div.split-windows-view
      [rgl {:id         "dashboard-widget-grid"
            :width      1200                                 ;<determined dynamically>
            ;:layout     [{:i "some-key" :x 0 :y 0 :w 20 :h 2} {:i "some-key2" :x 0 :y 1 :w 1 :h 2}]
            ;:data       [{:i "some-key" :x 0 :y 0 :w 2 :h 2} {:i "some-key2" :x 0 :y 1 :w 1 :h 2}]
            :row-height 20
            :cols       24
            :class      "split-window-view tile-journal"
            ;:on-change (fn [_])   ;handle-layout-change ;; persistance to backend
            :item-props {:class "widget-component"}}

       [:div.rgl1 {:key       :all-stats
                   :data-grid {:i "all-stats" :x 0 :y 0 :w 6 :h 19}}
        [stats/stats-view cmp-map]]

       [:div.rgl1 {:key       :daily-summaries-single
                   :data-grid {:i "daily-summaries-single" :x 6 :y 17 :w 6 :h 3}}
        [:div.stats
         [ds/daily-summaries-chart (:daily-summary-stats @observed) 200 put-fn]]]

       [:div.rgl1 {:key       :pomo-single
                   :data-grid {:i "pomo-single" :x 12 :y 17 :w 6 :h 3}}
        [:div.stats
         [cp/pomodoro-bar-chart (:pomodoro-stats @observed) 150 "Pomodoros" 5 put-fn]]]

       [:div.rgl1 {:key       :wc-single
                   :data-grid {                             ;:i "wc-single"
                               :x 19 :y 17 :w 6 :h 3}}
        [:div.stats
         [wc/wordcount-chart (:wordcount-stats @observed) 150 put-fn 1000]]]

       [:div.rgl1 {:key       :split-left
                   :data-grid {:i "split-left" :x 6 :y 0 :w 9 :h 16}}
        [split-window-view2 cmp-map :left]]

       [:div.rgl1 {:key       :split-right
                   :data-grid {:i "split-right" :x 15 :y 0 :w 9 :h 16}}
        [split-window-view2 cmp-map :right]]]]

     [n/new-entries-view store-snapshot local-cfg put-fn]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn split-windows-view
              :dom-id  "content"}))

(ns iwaswhere-web.ui.grid
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.journal :as j]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.search :as search]
            [re-frame.core :refer [subscribe]]))

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
           [search/search-field-view query-id put-fn])
         (when query-id
           [j/journal-view local-cfg put-fn])]))))

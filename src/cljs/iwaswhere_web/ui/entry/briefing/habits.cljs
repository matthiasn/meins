(ns iwaswhere-web.ui.entry.briefing.habits
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as up]
            [clojure.string :as s]))

(defn habit-sorter
  "Sorts tasks."
  [x y]
  (let [c (compare (get-in x [:habit :priority] :X)
                   (get-in y [:habit :priority] :X))]
    (if (not= c 0) c (compare (get-in y [:habit :points])
                              (get-in x [:habit :points])))))

(defn waiting-habits
  "Renders table with open entries, such as started tasks and open habits."
  [tab-group entry local-cfg put-fn]
  (let [cfg (subscribe [:cfg])
        query-cfg (subscribe [:query-cfg])
        waiting-habits (subscribe [:waiting-habits])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])
        entries-list
        (reaction
          (let [entries-map @entries-map
                find-missing (u/find-missing-entry entries-map put-fn)
                entries (->> @waiting-habits
                             (map (fn [ts] (find-missing ts)))
                             (sort habit-sorter))
                conf (merge @cfg @options)]
            (if (:show-pvt @cfg)
              entries
              (filter (u/pvt-filter conf entries-map) entries))))]
    (fn waiting-habits-list-render [tab-group entry local-cfg put-fn]
      (let [entries-list @entries-list
            today (.format (js/moment.) "YYYY-MM-DD")
            briefing-day (-> entry :briefing :day)]
        (when (and (= today briefing-day) (seq entries-list))
          [:div
           [:table.habits
            [:tbody
             [:tr
              [:th [:span.fa.fa-exclamation-triangle]]
              [:th [:span.fa.fa-diamond]]
              [:th "waiting habit"]]
             (for [entry entries-list]
               (let [ts (:timestamp entry)]
                 ^{:key ts}
                 [:tr {:on-click (up/add-search ts tab-group put-fn)}
                  [:td
                   (when-let [prio (-> entry :habit :priority)]
                     [:span.prio {:class prio} prio])]
                  [:td.award-points
                   (when-let [points (-> entry :habit :points)]
                     points)]
                  [:td.habit
                   (some-> entry
                           :md
                           (s/replace "#task" "")
                           (s/replace "#habit" "")
                           (s/replace "##" "")
                           s/split-lines
                           first)]]))]]])))))

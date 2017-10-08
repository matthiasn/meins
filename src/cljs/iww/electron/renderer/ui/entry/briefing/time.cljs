(ns iww.electron.renderer.ui.entry.briefing.time
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iww.electron.renderer.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as up]))

(defn time-by-stories
  "Render list of times spent on individual stories, plus the total."
  [day-stats local put-fn]
  (let [stories (subscribe [:stories])
        saga-filter (fn [[k v]]
                      (if-let [selected (:selected @local)]
                        (let [story (get @stories k)]
                          (= selected (:linked-saga story)))
                        true))
        story-name-mapper (fn [[k v]]
                            (let [s (or (:story-name (get @stories k)) "none")]
                              [k s v]))]
    (fn [day-stats local put-fn]
      (let [dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)
            time-by-story (:time-by-story day-stats)
            time-by-story2 (->> day-stats
                                :time-by-story
                                (filter saga-filter)
                                (map story-name-mapper)
                                (sort-by second))
            y-scale 0.0045]
        (when date
          [:table
           [:tbody
            [:tr [:th ""] [:th "story"] [:th "actual"]]
            (for [[id story v] time-by-story2]
              (let [color (cc/item-color story)
                    q (merge
                        (up/parse-search date)
                        {:story (when-not (js/isNaN id) id)})
                    click-fn (fn [_]
                               (put-fn [:search/add {:tab-group :right
                                                     :query     q}]))]
                ^{:key story}
                [:tr {:on-click click-fn}
                 [:td [:div.legend {:style {:background-color color}}]]
                 [:td [:strong story]]
                 [:td.time (u/duration-string v)]]))]])))))

(defn time-by-sagas
  [entry day-stats local edit-mode? put-fn]
  (let [sagas (subscribe [:sagas])
        time-alloc-input-fn
        (fn [entry saga]
          (fn [ev]
            (let [m (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  s (* m 60)
                  updated (assoc-in entry [:briefing :time-allocation saga] s)]
              (put-fn [:entry/update-local updated]))))
        filter-click #(swap! local update-in [:outstanding-time-filter] not)]
    (fn [entry day-stats local edit-mode? put-fn]
      (let [actual-times (:time-by-saga day-stats)
            filtered? (:outstanding-time-filter @local)
            filter-cls (when-not filtered? "inactive")
            sagas (sort-by #(-> % second :saga-name) @sagas)
            selected (:selected @local)]
        [:table
         [:tbody
          [:tr
           [:th [:span.fa.fa-filter
                 {:on-click filter-click
                  :class filter-cls}]]
           [:th "saga"]
           [:th "planned"]
           [:th "actual"]
           [:th "remaining"]]
          (for [[k v] sagas]
            (let [allocation (get-in entry [:briefing :time-allocation k] 0)
                  actual (get-in actual-times [k] 0)
                  remaining (- allocation actual)
                  color (cc/item-color (:saga-name v))
                  click
                  (fn [_]
                    (when-not edit-mode?
                      (swap! local update-in [:selected] #(if (= k %) nil k))))]
              (when (or (pos? allocation) (get actual-times k) edit-mode?)
                (when (and (or (not filtered?) (pos? remaining) edit-mode?)
                           (or (not selected) (= selected k)))
                  ^{:key (str :time-allocation k)}
                  [:tr {:on-click click
                        :class    (when (= k (:selected @local)) "selected")}
                   [:td [:div.legend {:style {:background-color color}}]]
                   [:td [:strong (:saga-name v)]]
                   [:td.time
                    (if edit-mode?
                      [:input {:on-input (time-alloc-input-fn entry k)
                               :value    (when allocation (/ allocation 60))
                               :type     :number}]
                      [:span (u/duration-string allocation)])]
                   [:td.time (u/duration-string actual)]
                   [:td.time [:strong (u/duration-string remaining)]]]))))]]))))

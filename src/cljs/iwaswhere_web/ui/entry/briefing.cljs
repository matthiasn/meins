(ns iwaswhere-web.ui.entry.briefing
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.charts.data :as cd]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.ui.entry.briefing.tasks :as tasks]
            [iwaswhere-web.ui.entry.briefing.habits :as habits]
            [iwaswhere-web.ui.entry.briefing.time :as time]
            [iwaswhere-web.utils.parse :as up]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn vertical-bar
  "Draws vertical stacked barchart."
  [entities k time-by-entities y-scale]
  (let [data (cd/time-by-entity-stacked time-by-entities)]
    (when (seq time-by-entities)
      [:svg.vertical-bar
       ;{:viewBox (str "0 0 12 300")}
       [:g (for [[entity {:keys [x v]}] data]
             (let [h (* y-scale v)
                   x (* y-scale x)
                   entity-name (or (k (get entities entity)) "none")]
               ^{:key (str entity)}
               [:rect {:fill   (cc/item-color entity-name)
                       :y      x
                       :x      0
                       :width  12
                       :height h}]))]])))

(defn briefing-view
  [entry put-fn edit-mode? local-cfg]
  (let [chart-data (subscribe [:chart-data])
        day (-> entry :briefing :day)
        today (.format (js/moment.) "YYYY-MM-DD")
        filter-btn (if (= day today) :active :open)
        local (r/atom {:filter  filter-btn
                       :outstanding-time-filter true
                       :on-hold false})
        stats (subscribe [:stats])
        options (subscribe [:options])
        sagas (reaction (:sagas @options))
        entries-map (subscribe [:entries-map])
        results (subscribe [:results])
        cfg (subscribe [:cfg])

        input-fn
        (fn [entry]
          (fn [ev]
            (let [day (-> ev .-nativeEvent .-target .-value)
                  updated (assoc-in entry [:briefing :day] day)]
              (put-fn [:entry/update-local updated]))))

        time-alloc-input-fn
        (fn [entry saga]
          (fn [ev]
            (let [m (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  s (* m 60)
                  updated (assoc-in entry [:briefing :time-allocation saga] s)]
              (put-fn [:entry/update-local updated]))))]
    (fn briefing-render [entry put-fn edit-mode? local-cfg]
      (when (contains? (:tags entry) "#briefing")
        (let [sagas @sagas
              ts (:timestamp entry)
              {:keys [pomodoro-stats task-stats wordcount-stats]} @chart-data
              day (-> entry :briefing :day)
              day-stats (get pomodoro-stats day)
              dur (u/duration-string (:total-time day-stats))
              word-stats (get wordcount-stats day)
              {:keys [tasks-cnt done-cnt closed-cnt]} (get task-stats day)
              started (:started-tasks-cnt @stats)
              allocation (-> entry :briefing :time-allocation)
              actual-times (:time-by-saga day-stats)
              remaining (cd/remaining-times actual-times allocation)
              past-7-days (cd/past-7-days :time-by-saga pomodoro-stats)
              tab-group (:tab-group local-cfg)]
          [:div.briefing
           [:form.briefing-details
            [:fieldset
             [:legend (or day "date not set")]
             (when edit-mode?
               [:div
                [:label " Day: "]
                [:input {:type     :date
                         :on-input (input-fn entry)
                         :value    day}]])
             (when tasks-cnt
               [:div
                "Tasks: " [:strong tasks-cnt] " created, "
                [:strong done-cnt] " done, "
                [:strong closed-cnt] " closed. "
                [:strong (or (:word-count word-stats) 0)] " words written."])
             [:div
              "Total planned: "
              [:strong
               (u/duration-string
                 (apply + (map second (-> entry :briefing :time-allocation))))]
              (when (seq dur)
                [:span
                 " Logged: " [:strong dur] " in " (:total day-stats) " entries."])]
             [time/time-by-sagas entry day-stats local edit-mode? put-fn]
             [tasks/started-tasks tab-group local put-fn]
             [tasks/open-linked-tasks ts local local-cfg put-fn]
             [habits/waiting-habits tab-group entry put-fn]
             (when day-stats [time/time-by-stories day-stats local put-fn])]]
           [:div.stacked-bars
            [:div [vertical-bar sagas :saga-name allocation 0.0045]]
            [:div [vertical-bar sagas :saga-name actual-times 0.0045]]
            [:div [vertical-bar sagas :saga-name remaining 0.0045]]]])))))

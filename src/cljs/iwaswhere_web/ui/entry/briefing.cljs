(ns iwaswhere-web.ui.entry.briefing
  (:require [matthiasn.systems-toolbox.component :as st]
            [iwaswhere-web.ui.charts.pomodoros :as p]
            [re-frame.core :refer [subscribe]]))

(defn briefing-view
  [entry put-fn edit-mode?]
  (let [chart-data (subscribe [:chart-data])
        input-fn
        (fn [entry]
          (fn [ev]
            (prn (-> ev .-nativeEvent .-target .-value))
            (let [day (-> ev .-nativeEvent .-target .-value)
                  updated (assoc-in entry [:briefing :day] day)]
              (put-fn [:entry/update-local updated]))))
        follow-up-select
        (fn [entry]
          (fn [ev]
            (let [sel (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  follow-up-hrs (when-not (js/isNaN sel) sel)
                  updated (assoc-in entry [:task :follow-up-hrs] follow-up-hrs)]
              (put-fn [:entry/update-local updated]))))]
    (fn briefing-render [entry put-fn edit-mode?]
      (let [{:keys [pomodoro-stats activity-stats task-stats wordcount-stats
                    daily-summary-stats media-stats]} @chart-data
            day (-> entry :briefing :day)
            day-stats (get pomodoro-stats day)
            {:keys [tasks-cnt done-cnt closed-cnt]} (get task-stats day)]
        (when (contains? (:tags entry) "#briefing")
          [:form.task-details
           [:fieldset
            [:legend "Briefing details"]
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
               [:strong closed-cnt] " closed"])
            (when day-stats [p/time-by-stories-list day-stats])]])))))

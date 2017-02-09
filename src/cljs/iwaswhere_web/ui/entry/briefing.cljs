(ns iwaswhere-web.ui.entry.briefing
  (:require [matthiasn.systems-toolbox.component :as st]
            [iwaswhere-web.ui.charts.pomodoros :as p]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.charts.data :as cd]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]))

(defn time-by-stories-list
  "Render list of times spent on individual stories, plus the total."
  [day-stats]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        books (reaction (:books @options))]
    (fn [day-stats]
      (let [stories @stories
            books @books
            dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)
            time-by-book (:time-by-book day-stats)
            time-by-story (:time-by-story day-stats)
            y-scale 0.0045]
        (when date
          [:div.story-time
           [:div "Logged: " [:strong dur] " in " (:total day-stats) " entries."]
           [:hr]
           [cc/horizontal-bar stories :story-name time-by-story y-scale]
           (for [[story v] (:time-by-story day-stats)]
             (let [story-name (or (:story-name (get stories story)) "none")]
               ^{:key story}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color story-name)}}]
                [:strong.name story-name] (u/duration-string v)]))
           [:hr]
           [cc/horizontal-bar books :book-name time-by-book y-scale]
           (for [[book v] (:time-by-book day-stats)]
             (let [book-name (or (:book-name (get books book)) "none")]
               ^{:key book}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color book-name)}}]
                [:strong.name book-name] (u/duration-string v)]))])))))

(defn briefing-view
  [entry put-fn edit-mode?]
  (let [chart-data (subscribe [:chart-data])
        stats (subscribe [:stats])
        options (subscribe [:options])
        books (reaction (:books @options))
        input-fn
        (fn [entry]
          (fn [ev]
            (let [day (-> ev .-nativeEvent .-target .-value)
                  updated (assoc-in entry [:briefing :day] day)]
              (put-fn [:entry/update-local updated]))))
        time-alloc-input-fn
        (fn [entry book]
          (fn [ev]
            (let [m (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  s (* m 60)
                  updated (assoc-in entry [:briefing :time-allocation book] s)]
              (put-fn [:entry/update-local updated]))))]
    (fn briefing-render [entry put-fn edit-mode?]
      (when (contains? (:tags entry) "#briefing")
        (let [books @books
              {:keys [pomodoro-stats activity-stats task-stats wordcount-stats
                      daily-summary-stats media-stats]} @chart-data
              day (-> entry :briefing :day)
              day-stats (get pomodoro-stats day)
              word-stats (get wordcount-stats day)
              {:keys [tasks-cnt done-cnt closed-cnt]} (get task-stats day)
              started (:started-tasks-cnt @stats)
              allocation (-> entry :briefing :time-allocation)
              remaining (cd/remaining-times (:time-by-book day-stats) allocation)
              past-7-days (cd/past-7-days pomodoro-stats :time-by-book)]
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
               [:strong closed-cnt] " closed"])
            (when word-stats
              [:div
               [:strong (:started-tasks-cnt @stats)] " started tasks, "
               [:strong (:word-count word-stats)] " words written."])
            (when day-stats [time-by-stories-list day-stats])
            [:hr]
            [:div [cc/horizontal-bar books :book-name allocation 0.0045]]
            [:div [cc/horizontal-bar books :book-name remaining 0.0045]]
            [:div
             "Total planned: "
             [:strong
              (u/duration-string
                (apply + (map second (-> entry :briefing :time-allocation))))]]
            [:div.story-time
             (for [[k v] books]
               (let [allocation (get-in entry [:briefing :time-allocation k] 0)
                     actual (get-in (:time-by-book day-stats) [k] 0)
                     remaining (- allocation actual)]
                 ^{:key (str :time-allocation k)}
                 [:div
                  (when (or (pos? allocation) edit-mode?)
                    [:div
                     [:span.legend
                      {:style {:background-color (cc/item-color (:book-name v))}}]
                     [:strong.name (:book-name v)]
                     (if edit-mode?
                       [:input {:on-input (time-alloc-input-fn entry k)
                                :value    (when allocation (/ allocation 60))
                                :type     :number}]
                       (when allocation
                         [:span.allocated (u/duration-string allocation)]))
                     (when (pos? remaining)
                       [:span (u/duration-string remaining)])])]))]
            [:hr]
            [:div [cc/horizontal-bar books :book-name past-7-days 0.001]]
            [:div
             "Past seven days: "
             [:strong (u/duration-string (apply + (map second past-7-days)))]]
            [:div.story-time
             (for [[book v] past-7-days]
               (let [book-name (or (:book-name (get books book)) "none")]
                 ^{:key book}
                 [:div
                  [:span.legend
                   {:style {:background-color (cc/item-color book-name)}}]
                  [:strong.name book-name] (u/duration-string v)]))]]])))))

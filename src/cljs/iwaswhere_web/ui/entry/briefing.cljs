(ns iwaswhere-web.ui.entry.briefing
  (:require [matthiasn.systems-toolbox.component :as st]
            [iwaswhere-web.ui.charts.pomodoros :as p]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]))

(defn time-by-stories-list
  "Render list of times spent on individual stories, plus the total."
  [day-stats]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        books (reaction (:books @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :x] total))))]
    (fn [day-stats]
      (let [stories @stories
            books @books
            dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)
            time-by-book (sort-by #(str (first %)) (:time-by-book day-stats))
            stacked-by-book (reduce stacked-reducer {} time-by-book)
            time-by-book (reverse (sort-by #(str (first %)) (:items stacked-by-book)))
            time-by-story (sort-by #(str (first %)) (:time-by-story day-stats))
            stacked-by-story (reduce stacked-reducer {} time-by-story)
            time-by-story (reverse (sort-by #(str (first %)) (:items stacked-by-story)))
            y-scale 0.0045]
        (when date
          [:div.story-time
           [:div "Logged: " [:strong dur] " in " (:total day-stats) " entries."]
           [:hr]
           [:svg
            {:viewBox (str "0 0 300 15")}
            [:g (for [[book {:keys [x v]}] time-by-book]
                  (let [w (* y-scale v)
                        x (* y-scale x)
                        book-name (or (:book-name (get books book)) "No book")]
                    ^{:key (str book)}
                    [:rect {:fill    (cc/item-color book-name)
                            :y       0
                            :x       x
                            :width   w
                            :height  15}]))]]
           (for [[book v] (:time-by-book day-stats)]
             (let [book-name (or (:book-name (get books book)) "No book")]
               ^{:key book}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color book-name)}}]
                [:strong book-name] ": " (u/duration-string v)]))
           [:hr]
           [:svg
            {:viewBox (str "0 0 300 15")}
            [:g (for [[story {:keys [x v]}] time-by-story]
                  (let [w (* y-scale v)
                        x (* y-scale x)
                        story-name (or (:story-name (get stories story)) "No story")]
                    ^{:key (str story)}
                    [:rect {:fill    (cc/item-color story-name)
                            :y       0
                            :x       x
                            :width   w
                            :height  15}]))]]
           (for [[story v] (:time-by-story day-stats)]
             (let [story-name (or (:story-name (get stories story)) "No story")]
               ^{:key story}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color story-name)}}]
                [:strong story-name] ": " (u/duration-string v)]))])))))

(defn briefing-view
  [entry put-fn edit-mode?]
  (let [chart-data (subscribe [:chart-data])
        stats (subscribe [:stats])
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
            word-stats (get wordcount-stats day)
            {:keys [tasks-cnt done-cnt closed-cnt]} (get task-stats day)
            started (:started-tasks-cnt @stats)]
        (when (contains? (:tags entry) "#briefing")
          [:form.task-details
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
               [:strong (:word-count word-stats)] " words written." ])
            (when day-stats [time-by-stories-list day-stats])]])))))

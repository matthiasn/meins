(ns iwaswhere-web.ui.pomodoro
  "This namespace holds the fucntions for rendering the text (markdown) content of a journal entry.
  This includes both a properly styled element for static content and the edit-mode view, with
  autosuggestions for tags and mentions."
  (:require [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.helpers :as h]
            [reagent.core :as r]))

(defn pomodoro-defaults
  [ts]
  {:comment-for    ts
   :entry-type     :pomodoro
   :planned-dur    15 ; 25 min
   :completed-time 0
   :interruptions  0})

(defn duration-string
  "Format duration string from seconds."
  [seconds]
  (let [hours (.floor js/Math (/ seconds 3600))
        seconds (rem seconds 3600)
        min (.floor js/Math (/ seconds 60))
        sec (rem seconds 60)]
    (str (when (pos? hours) (str hours "h "))
         (when (pos? min) (str min "m "))
         (when (pos? sec) (str sec "s")))))

(defn pomodoro-header
  [entry put-fn]
  (let [timeout (r/atom nil)
        cached-entry (atom entry)]
    (fn [entry put-fn]
      (reset! cached-entry entry)
      (let [time-left? #(> (:planned-dur %) (:completed-time %))
            clear-clock #(do (.clearTimeout js/window @timeout)
                             (reset! timeout nil))
            interval-fn (fn []
                          (if (time-left? @cached-entry)
                            (put-fn [:cmd/pomodoro-inc entry])
                            (do (clear-clock)
                                (.setTimeout js/window
                                            #(put-fn [:text-entry/update (h/clean-entry @cached-entry)])
                                            5000))))
            start-stop-fn (fn [_ev]
                            (if @timeout
                              (do (clear-clock)
                                  (put-fn [:entry/update-local (update-in @cached-entry [:interruptions] inc)]))
                              (reset! timeout (.setInterval js/window interval-fn 1000))))]
        [:div.pomodoro
         [:strong (if (time-left? entry) "Pomodoro: " "Pomodoro completed: ")]
         [:span.dur (duration-string (:completed-time entry))]
         (when (and (time-left? entry) (:new-entry entry))
           [:span.btn {:on-click start-stop-fn
                       :class    (if @timeout "stop" "start")}
            [:span.fa {:class (if @timeout "fa-pause-circle-o" "fa-play-circle-o")}]
            (if @timeout " pause" " start")])]))))

(defn pomodoro-stats-view
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [pomodoros (filter #(= :pomodoro (:entry-type %)) entries)
        completed-pomodoros (filter #(= (:planned-dur %) (:completed-time %)) pomodoros)
        total-time (reduce + (map :completed-time pomodoros))
        interruptions (reduce + (map :interruptions pomodoros))]
    (when (seq pomodoros)
      [:div (str "In result: " (count completed-pomodoros) " out of " (count pomodoros) " pomodoros completed, "
                 (duration-string total-time) " logged. Interruptions: " interruptions)])))

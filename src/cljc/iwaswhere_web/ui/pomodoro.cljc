(ns iwaswhere-web.ui.pomodoro
  "This namespace holds the functions for rendering the text (markdown) content
   of a journal entry. This includes both a properly styled element for static
   content and the edit-mode view, with autosuggestions for tags and mentions."
  (:require [iwaswhere-web.utils.misc :as u]))

(defn pomodoro-defaults [ts]
  {:comment-for    ts
   :entry-type     :pomodoro
   :planned-dur    1500  ; 25 min
   :completed-time 0})

(defn pomodoro-header [entry start-fn edit-mode?]
  (let [running? (:pomodoro-running entry)
        completed-time (:completed-time entry)]
    (when (= (:entry-type entry) :pomodoro)
      [:div.pomodoro
       [:span.fa.fa-clock-o.completed]
       (when (pos? completed-time)
         [:span.dur (u/duration-string completed-time)])
       (when edit-mode?
         [:span.btn {:on-click start-fn
                     :class    (if running? "stop" "start")}
          [:span.fa
           {:class (if running? "fa-pause-circle-o" "fa-play-circle-o")}]
          (if running? "pause" "start")])])))

(defn pomodoro-stats [entries]
  (let [pomodoros (filter #(= :pomodoro (:entry-type %)) entries)
        completed (filter #(= (:planned-dur %) (:completed-time %)) pomodoros)]
    {:pomodoros           (count pomodoros)
     :completed-pomodoros (count completed)
     :total-time          (reduce + (map :completed-time pomodoros))}))

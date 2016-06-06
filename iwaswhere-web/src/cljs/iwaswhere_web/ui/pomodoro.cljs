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
   :planned-dur    1500  ; 25 min
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
  "Header showing time done, plus controls when not completed."
  [entry put-fn edit-mode?]
  (let [time-left? #(> (:planned-dur %) (:completed-time %))
        running? (:pomodoro-running entry)
        start-fn #(put-fn [:cmd/pomodoro-start entry])]
    [:div.pomodoro
     [:strong (if (time-left? entry) "Pomodoro: " "Pomodoro completed: ")]
     [:span.dur (duration-string (:completed-time entry))]
     (when (and edit-mode? (time-left? entry))
       [:span.btn {:on-click start-fn :class (if running? "stop" "start")}
        [:span.fa {:class (if running? "fa-pause-circle-o" "fa-play-circle-o")}]
        (if running? " pause" " start")])]))

(defn pomodoro-stats-str
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [pomodoros (filter #(= :pomodoro (:entry-type %)) entries)
        completed-pomodoros (filter #(= (:planned-dur %) (:completed-time %)) pomodoros)
        total-time (reduce + (map :completed-time pomodoros))
        interruptions (reduce + (map :interruptions pomodoros))
        interruptions-str (when (pos? interruptions) (str ". Interruptions: " interruptions))]
    (when (seq pomodoros)
      (if (= pomodoros completed-pomodoros)
        (str "Pomodoros: " (count completed-pomodoros) ", " (duration-string total-time) interruptions-str)
        (str "Pomodoros: " (count completed-pomodoros) " of " (count pomodoros) " completed, "
             (duration-string total-time) interruptions-str)))))

(defn pomodoro-stats-view
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [pomodoros (filter #(= :pomodoro (:entry-type %)) entries)
        completed-pomodoros (filter #(= (:planned-dur %) (:completed-time %)) pomodoros)
        completed-count (count completed-pomodoros)
        total-time (reduce + (map :completed-time pomodoros))
        interruptions (reduce + (map :interruptions pomodoros))
        interruptions-str (when (pos? interruptions) (str ". Interruptions: " interruptions))]
    (when (seq pomodoros)
      [:span
       (into [:span] (map (fn [_] [:span.fa.fa-clock-o.completed]) (range completed-count)))
       (into [:span] (map (fn [_] [:span.fa.fa-clock-o.incomplete]) (range (- (count pomodoros) completed-count))))
       (str " " (duration-string total-time) interruptions-str)])))

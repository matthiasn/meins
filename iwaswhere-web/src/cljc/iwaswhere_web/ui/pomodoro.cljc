(ns iwaswhere-web.ui.pomodoro
  "This namespace holds the fucntions for rendering the text (markdown) content of a journal entry.
  This includes both a properly styled element for static content and the edit-mode view, with
  autosuggestions for tags and mentions."
  (:require [iwaswhere-web.ui.utils :as u]))

(defn pomodoro-defaults
  [ts]
  {:comment-for    ts
   :entry-type     :pomodoro
   :planned-dur    1500  ; 25 min
   :completed-time 0
   :interruptions  0})

(defn pomodoro-header
  "Header showing time done, plus controls when not completed."
  [entry start-fn edit-mode?]
  (let [time-left? #(> (:planned-dur %) (:completed-time %))
        running? (:pomodoro-running entry)
        completed-time (:completed-time entry)
        interruptions (:interruptions entry)]
    (when (= (:entry-type entry) :pomodoro)
      [:div.pomodoro
       (if (time-left? entry) [:span.fa.fa-clock-o.incomplete]
                              [:span.fa.fa-clock-o.completed])
       (when (pos? completed-time) [:span.dur (u/duration-string completed-time)])
       (if (<= interruptions 3)
         (into [:span] (map (fn [_] [:span.fa.fa-bolt]) (range interruptions)))
         [:span [:span.fa.fa-bolt] [:span.bolt-cnt interruptions]])
       (when (and edit-mode? (time-left? entry))
         [:span.btn {:on-click start-fn :class (if running? "stop" "start")}
          [:span.fa {:class (if running? "fa-pause-circle-o" "fa-play-circle-o")}]
          (if running? " pause" " start")])])))

(defn pomodoro-stats
  "Create some stats about the provided entries."
  [entries]
  (let [pomodoros (filter #(= :pomodoro (:entry-type %)) entries)
        completed-pomodoros (filter #(= (:planned-dur %) (:completed-time %)) pomodoros)
        interruptions (reduce + (map :interruptions pomodoros))
        interruptions-str (when (pos? interruptions) (str ". Interruptions: " interruptions))]
    {:pomodoros           (count pomodoros)
     :completed-pomodoros (count completed-pomodoros)
     :total-time          (reduce + (map :completed-time pomodoros))
     :interruptions       interruptions
     :interruptions-str   interruptions-str}))

(defn pomodoro-stats-str
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [{:keys [pomodoros completed-pomodoros total-time interruptions-str]} (pomodoro-stats entries)]
    (when (pos? pomodoros)
      (if (= pomodoros completed-pomodoros)
        (str "Pomodoros: " completed-pomodoros ", " (u/duration-string total-time) interruptions-str)
        (str "Pomodoros: " completed-pomodoros " of " pomodoros " completed, "
             (u/duration-string total-time) interruptions-str)))))

(defn pomodoro-stats-view
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [{:keys [pomodoros completed-pomodoros total-time interruptions]} (pomodoro-stats entries)
        incomplete-pomodoros (- pomodoros completed-pomodoros)]
    (when (pos? pomodoros)
      [:span
       (if (<= completed-pomodoros 3)
         (into [:span] (map (fn [_] [:span.fa.fa-clock-o.completed]) (range completed-pomodoros)))
         [:span [:span.fa.fa-clock-o.completed] [:span.completed-cnt completed-pomodoros]])
       (if (<= incomplete-pomodoros 3)
         (into [:span] (map (fn [_] [:span.fa.fa-clock-o.incomplete]) (range incomplete-pomodoros)))
         [:span [:span.fa.fa-clock-o.incomplete] [:span.incomplete-cnt incomplete-pomodoros]])
       [:span.dur (u/duration-string total-time)]
       (if (<= interruptions 3)
         (into [:span] (map (fn [_] [:span.fa.fa-bolt]) (range interruptions)))
         [:span [:span.fa.fa-bolt] [:span.bolt-cnt interruptions]])])))

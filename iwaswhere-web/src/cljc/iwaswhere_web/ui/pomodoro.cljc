(ns iwaswhere-web.ui.pomodoro
  "This namespace holds the fucntions for rendering the text (markdown) content of a journal entry.
  This includes both a properly styled element for static content and the edit-mode view, with
  autosuggestions for tags and mentions.")

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
  (let [hours (int (/ seconds 3600))
        seconds (rem seconds 3600)
        min (int (/ seconds 60))
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
     :interruptions       (reduce + (map :interruptions pomodoros))
     :interruptions-str   interruptions-str}))

(defn pomodoro-stats-str
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [{:keys [pomodoros completed-pomodoros total-time interruptions-str]} (pomodoro-stats entries)]
    (when (pos? pomodoros)
      (if (= pomodoros completed-pomodoros)
        (str "Pomodoros: " completed-pomodoros ", " (duration-string total-time) interruptions-str)
        (str "Pomodoros: " completed-pomodoros " of " pomodoros " completed, "
             (duration-string total-time) interruptions-str)))))

(defn pomodoro-stats-view
  "Shows some information about the number of pomodoros created and completed on any given day, where
  completion is achieved when the :completed-time equals the planned duration :planned-dur.
  Also, the total time logged via pomodoros is shown."
  [entries]
  (let [{:keys [pomodoros completed-pomodoros total-time interruptions-str]} (pomodoro-stats entries)]
    (when (pos? pomodoros)
      [:span
       (into [:span] (map (fn [_] [:span.fa.fa-clock-o.completed]) (range completed-pomodoros)))
       (into [:span] (map (fn [_] [:span.fa.fa-clock-o.incomplete]) (range (- pomodoros completed-pomodoros))))
       (str " " (duration-string total-time) interruptions-str)])))

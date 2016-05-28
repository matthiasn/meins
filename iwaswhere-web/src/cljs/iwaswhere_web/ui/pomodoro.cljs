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
   :planned-dur    1500 ; 25 min
   :completed-time 0
   :interruptions  0})

(defn pomodoro-header
  [entry put-fn]
  (let [timeout (r/atom nil)
        cached-entry (atom entry)]
    (fn [entry put-fn]
      (reset! cached-entry entry)
      (let [time-left? #(> (:planned-dur %) (:completed-time %))
            completed (:completed-time entry)
            min (.floor js/Math (/ completed 60))
            sec (rem completed 60)
            dur-str (str (when (pos? min) (str min "m ")) (when (pos? sec) (str sec "s ")))
            ringer-id (str "ring-" (:timestamp entry))
            clear-clock #(do (.clearTimeout js/window @timeout)
                             (reset! timeout nil))
            interval-fn (fn []
                          (if (time-left? @cached-entry)
                            (put-fn [:entry/update-local {:timestamp      (:timestamp entry)
                                                          :completed-time (inc (:completed-time @cached-entry))}])
                            (do (clear-clock)
                                (.play (.getElementById js/document ringer-id)))))
            start-stop-fn (fn [_ev] (if @timeout (clear-clock)
                                                 (reset! timeout (.setInterval js/window interval-fn 1000))))]
        [:div.pomodoro
         ;; Currently, sounds from http://www.orangefreesounds.com/old-clock-ringing-short/
         ;; TODO: record own alarm clock
         [m/audioplayer "/mp3/old-clock-ringing-short.mp3" false false ringer-id]
         (when @timeout [m/audioplayer "/mp3/ticking-clock-sound.mp3" true true])
         [:strong (if (time-left? entry) "Pomodoro: " "Pomodoro completed: ")]
         [:span dur-str]
         (when (and (time-left? entry) (:new-entry entry))
           [:span.btn {:on-click start-stop-fn
                       :class    (if @timeout "stop" "start")}
            [:span.fa {:class (if @timeout "fa-pause-circle-o" "fa-play-circle-o")}]
            (if @timeout " pause" " start")])]))))

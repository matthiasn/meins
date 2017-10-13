(ns iww.electron.renderer.ui.entry.pomodoro
  (:require [iwaswhere-web.utils.misc :as u]
            [reagent.core :as r]))

(defn pomodoro-header [entry start-fn edit-mode? put-fn]
  (let [local (r/atom {:edit false})
        click #(swap! local assoc-in [:edit] true)
        on-change (fn [ev]
                    (let [v (.. ev -target -value)
                          parsed (when (seq v) (js/parseInt v))
                          updated (assoc-in @entry [:completed-time] parsed)]
                      (put-fn [:entry/update-local updated])))]
    (fn [entry start-fn edit-mode? put-fn]
      (let [running? (:pomodoro-running @entry)
            completed-time (:completed-time @entry)]
        (when (= (:entry-type @entry) :pomodoro)
          [:div.pomodoro
           [:span.fa.fa-clock-o.completed]
           (when (pos? completed-time)
             (if (and edit-mode? (:edit @local))
               [:input {:value     completed-time
                        :type      :number
                        :on-change on-change}]
               [:span.dur {:on-click click}
                (u/duration-string completed-time)]))
           (when edit-mode?
             [:span.btn {:on-click start-fn
                         :class    (if running? "stop" "start")}
              [:span.fa
               {:class (if running? "fa-pause-circle-o" "fa-play-circle-o")}]
              (if running? "pause" "start")])])))))
(ns iwaswhere-web.ui.entry.task
  (:require [iwaswhere-web.ui.pikaday :as pikaday]))

(defn task-details
  [entry put-fn]
  (let [callback (fn [entry k]
                   (fn [inst]
                     (let [millis (.getTime inst)
                           updated (assoc-in entry [:task k] millis)]
                       (put-fn [:entry/update-local updated]))))]
    (fn [entry put-fn]
      (when (contains? (:tags entry) "#task")
        [:form.task-details
         [:fieldset
          [:legend "Task details"]
          [:span "Start date: "]
          [pikaday/date-selector
           {:date     (some-> entry :task :start (js/Date.))
            :callback (callback entry :start)}]
          [:span " Due date: "]
          [pikaday/date-selector
           {:date     (some-> entry :task :due (js/Date.))
            :callback (callback entry :due)}]]]))))
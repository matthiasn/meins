(ns iwaswhere-web.ui.entry.task)

(defn task-details
  [entry put-fn edit-mode?]
  (let [format-time #(.format (js/moment %) "ddd MMM DD - HH:mm")
        format-time2 #(.format (js/moment %) "YYYY-MM-DDTHH:mm")
        input-fn (fn [entry k]
                   (fn [ev]
                     (let [dt (js/moment (-> ev .-nativeEvent .-target .-value))
                           updated (assoc-in entry [:task k] (.valueOf dt))]
                       (put-fn [:entry/update-local updated]))))]
    (fn [entry put-fn edit-mode?]
      (when (contains? (:tags entry) "#task")
        [:form.task-details
         [:fieldset
          [:legend "Task details"]
          [:div
           [:span "Start: "]
           (if edit-mode?
             [:input {:type     :datetime-local
                      :on-input (input-fn entry :start)
                      :value    (format-time2 (-> entry :task :start))}]
             [:time (format-time (-> entry :task :start))])]
          [:div
           [:span " Due: "]
           (if edit-mode?
             [:input {:type     :datetime-local
                      :on-input (input-fn entry :due)
                      :value    (format-time2 (-> entry :task :due))}]
             [:time (format-time (-> entry :task :due))])]]]))))

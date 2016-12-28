(ns iwaswhere-web.ui.entry.task)

(defn task-details
  [entry put-fn edit-mode?]
  (let [format-time #(.format (js/moment %) "ddd MMM DD - HH:mm")
        format-time2 #(.format (js/moment %) "YYYY-MM-DDTHH:mm")
        input-fn
        (fn [entry k]
          (fn [ev]
            (let [dt (js/moment (-> ev .-nativeEvent .-target .-value))
                  updated (assoc-in entry [:task k] (.valueOf dt))]
              (put-fn [:entry/update-local updated]))))
        follow-up-select
        (fn [entry]
          (fn [ev]
            (let [sel (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  updated (assoc-in entry [:task :follow-up-hrs] sel)]
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
             [:time (format-time (-> entry :task :due))])]
          (if-let [follow-up-scheduled (:follow-up-scheduled (:task entry))]
            [:div "Follow-up in " follow-up-scheduled]
            [:div
             [:span "Follow-up after "]
             [:select {:value     (get-in entry [:task :follow-up-hrs])
                       :on-change (follow-up-select entry)}
              [:option ""]
              [:option {:value 1} "1"]
              [:option {:value 3} "3"]
              [:option {:value 6} "6"]
              [:option {:value 12} "12"]
              [:option {:value 24} "24"]
              [:option {:value 48} "48"]]
             [:span "hours"]])]]))))

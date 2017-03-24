(ns iwaswhere-web.ui.entry.task
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]))

(defn format-time2 [m]
  (.format (js/moment m) "YYYY-MM-DDTHH:mm"))

(defn task-details
  [entry put-fn edit-mode?]
  (let [format-time #(.format (js/moment %) "ddd MMM DD - HH:mm")
        input-fn (fn [entry k]
                   (fn [ev]
                     (let [dt (js/moment (-> ev .-nativeEvent .-target .-value))
                           updated (assoc-in entry [:task k] (.valueOf dt))]
                       (put-fn [:entry/update-local updated]))))
        set-active-from (fn [entry]
                          (fn [ev]
                            (let [dt (-> ev .-nativeEvent .-target .-value)
                                  updated (assoc-in entry [:task :active-from] dt)]
                              (put-fn [:entry/update-local updated]))))
        prio-select (fn [entry]
                      (fn [ev]
                        (let [sel (keyword (-> ev .-nativeEvent .-target .-value))
                              updated (assoc-in entry [:task :priority] sel)]
                          (put-fn [:entry/update-local updated]))))
        hold (fn [entry]
               (fn [ev]
                 (let [updated (update-in entry [:task :on-hold] not)]
                   (put-fn [:entry/update updated]))))]
    (fn [entry put-fn edit-mode?]
      (when (contains? (:tags entry) "#task")
        (when (and edit-mode? (not (:task entry)))
          (let [d (* 24 60 60 1000)
                now (st/now)
                updated (assoc-in entry [:task] {:due (+ now d d)})]
            (put-fn [:entry/update-local updated])))
        [:form.task-details
         [:fieldset
          [:legend "Task details"]
          [:div
           [:span " Due: "]
           (if edit-mode?
             [:input {:type     :datetime-local
                      :on-input (input-fn entry :due)
                      :value    (format-time2 (-> entry :task :due))}]
             [:time (format-time (-> entry :task :due))])]
          [:div
           [:span " Priority: "]
           [:select {:value     (get-in entry [:task :priority] "")
                     :disabled  (not edit-mode?)
                     :on-change (prio-select entry)}
            [:option ""]
            [:option {:value :A} "A"]
            [:option {:value :B} "B"]
            [:option {:value :C} "C"]
            [:option {:value :D} "D"]
            [:option {:value :E} "E"]]]
          [:div
           [:label "On hold? "]
           [:input {:type      :checkbox
                    :checked   (get-in entry [:task :on-hold])
                    :on-change (hold entry)}]]
          (let [active-from (get-in entry [:task :active-from])]
            (when (or edit-mode? active-from)
              [:div
               [:label "Active from: "]
               [:input {:type      :datetime-local
                        :read-only (not edit-mode?)
                        :on-input  (set-active-from entry)
                        :value     (or active-from "")}]]))]]))))

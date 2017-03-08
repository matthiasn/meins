(ns iwaswhere-web.ui.entry.task
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]))

(defn format-time2 [m]
  (.format (js/moment m) "YYYY-MM-DDTHH:mm"))

(defn hh-mm [m]
  (.format (js/moment m) "HH:mm"))

(defn ymd [m]
  (.format (js/moment m) "YYYY-MM-DD"))

(defn task-details
  [entry put-fn edit-mode?]
  (let [format-time #(.format (js/moment %) "ddd MMM DD - HH:mm")
        input-fn (fn [entry k]
                   (fn [ev]
                     (let [dt (js/moment (-> ev .-nativeEvent .-target .-value))
                           updated (assoc-in entry [:task k] (.valueOf dt))]
                       (put-fn [:entry/update-local updated]))))
        active-from (fn [entry]
                      (fn [ev]
                        (let [dt (-> ev .-nativeEvent .-target .-value)
                              updated (assoc-in entry [:task :active-from] dt)]
                          (put-fn [:entry/update-local updated]))))
        follow-up-select
        (fn [entry]
          (fn [ev]
            (let [sel (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  follow-up-hrs (when-not (js/isNaN sel) sel)
                  updated (assoc-in entry [:task :follow-up-hrs] follow-up-hrs)]
              (put-fn [:entry/update-local updated]))))
        prio-select (fn [entry]
                      (fn [ev]
                        (let [sel (keyword (-> ev .-nativeEvent .-target .-value))
                              updated (assoc-in entry [:task :priority] sel)]
                          (put-fn [:entry/update-local updated]))))]
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
           [:label "Active from: "]
           [:input {:type      :datetime-local
                    :read-only (not edit-mode?)
                    :on-input  (active-from entry)
                    :value     (get-in entry [:task :active-from])}]]
          (if-let [follow-up-scheduled (:follow-up-scheduled (:task entry))]
            [:div "Follow-up in " follow-up-scheduled]
            [:div
             [:span "Follow-up after "]
             [:select {:value     (get-in entry [:task :follow-up-hrs] "")
                       :on-change (follow-up-select entry)
                       :disabled  (not edit-mode?)}
              [:option ""]
              [:option {:value 1} "1 hour"]
              [:option {:value 3} "3 hours"]
              [:option {:value 6} "6 hours"]
              [:option {:value 12} "12 hours"]
              [:option {:value 18} "18 hours"]
              [:option {:value 24} "24 hours"]
              [:option {:value 48} "48 hours"]
              [:option {:value 72} "3 days"]
              [:option {:value 96} "4 days"]
              [:option {:value 168} "1 week"]]])]]))))

(defn next-habit-entry
  "Generate next habit entry, as appropriate at the time of calling.
   Store this to actually create entry."
  [entry]
  (let [next-hh-mm (-> entry
                       :habit
                       :active-from
                       (js/moment)
                       (hh-mm))
        active-days (filter identity (map (fn [[k v]] (when v k)) (get-in entry [:habit :days])))
        active-days (concat active-days (map #(+ % 7) active-days))
        active-days (filter number? active-days)
        current-day (.day (js/moment))
        next-day-int (first (drop-while #(>= current-day %) active-days))
        next-day (ymd (.add (js/moment) (- next-day-int current-day) "d"))
        next-active (str next-day "T" next-hh-mm)]
    (-> entry
        (assoc-in [:timestamp] (st/now))
        (assoc-in [:habit :active-from] next-active)
        (assoc-in [:habit :done] false)
        (dissoc :linked-entries-list)
        (dissoc :last-saved)
        (dissoc :latitude)
        (dissoc :longitude))))

(defn habit-details
  [entry put-fn edit-mode?]
  (let [active-from (fn [entry]
                      (fn [ev]
                        (let [dt (-> ev .-nativeEvent .-target .-value)
                              updated (assoc-in entry [:habit :active-from] dt)]
                          (prn updated)
                          (put-fn [:entry/update-local updated]))))
        day-select (fn [entry day]
                     (fn [ev]
                       (let [v (-> ev .-nativeEvent .-target .-value)
                             updated (update-in entry [:habit :days day] not)]
                         (put-fn [:entry/update-local updated]))))
        day-checkbox (fn [entry day]
                       [:input {:type      :checkbox
                                :checked   (get-in entry [:habit :days day])
                                :on-change (day-select entry day)}])
        done
        (fn [entry]
          (fn [ev]
            (if-not (-> entry :habit :next-entry)
              ;; check off and create next habit entry
              (let [next-entry (next-habit-entry entry)
                    updated (-> entry
                                (update-in [:habit :done] not)
                                (assoc-in [:habit :next-entry] (:timestamp next-entry)))]
                (put-fn [:entry/update next-entry])
                (h/send-w-geolocation next-entry put-fn)
                (put-fn [:entry/update updated]))
              ;; otherwise just toggle - follow-up is scheduled already
              (let [updated (update-in entry [:habit :done] not)]
                (put-fn [:entry/update updated])))))
        priority-select
        (fn [entry]
          (fn [ev]
            (let [sel (keyword (-> ev .-nativeEvent .-target .-value))
                  updated (assoc-in entry [:habit :priority] sel)]
              (put-fn [:entry/update-local updated]))))]
    (fn [entry put-fn edit-mode?]
      (when (contains? (:tags entry) "#habit")
        [:form.task-details
         [:fieldset
          [:legend "Habit details"]
          [:div
           [:label "Sun"] [day-checkbox entry 0]
           [:label "Mon"] [day-checkbox entry 1]
           [:label "Tue"] [day-checkbox entry 2]
           [:label "Wed"] [day-checkbox entry 3]
           [:label "Thu"] [day-checkbox entry 4]
           [:label "Fri"] [day-checkbox entry 5]
           [:label "Sat"] [day-checkbox entry 6]]
          [:div
           [:span " Priority: "]
           [:select {:value     (get-in entry [:habit :priority] "")
                     :disabled  (not edit-mode?)
                     :on-change (priority-select entry)}
            [:option ""]
            [:option {:value :A} "A"]
            [:option {:value :B} "B"]
            [:option {:value :C} "C"]
            [:option {:value :D} "D"]
            [:option {:value :E} "E"]]]
          [:div
           [:label "Active from: "]
           [:input {:type      :datetime-local
                    :read-only (not edit-mode?)
                    :on-input  (active-from entry)
                    :value     (get-in entry [:habit :active-from])}]]
          [:div [:label "Done? "] [:input {:type      :checkbox
                                           :checked   (get-in entry [:habit :done])
                                           :on-change (done entry)}]]]]))))

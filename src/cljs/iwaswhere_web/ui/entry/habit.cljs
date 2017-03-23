(ns iwaswhere-web.ui.entry.habit
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]))

(defn hh-mm [m]
  (.format (js/moment m) "HH:mm"))

(defn ymd [m]
  (.format (js/moment m) "YYYY-MM-DD"))

(defn next-habit-entry
  "Generate next habit entry, as appropriate at the time of calling.
   Store this to actually create entry."
  [entry]
  (let [next-hh-mm (-> entry :habit :active-from (js/moment) (hh-mm))
        active-days (filter identity (map (fn [[k v]] (when v k))
                                          (get-in entry [:habit :days])))
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
          [:div
           [:label "Done? "]
           [:input {:type      :checkbox
                    :checked   (get-in entry [:habit :done])
                    :on-change (done entry)}]]]]))))

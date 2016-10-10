(ns iwaswhere-web.ui.entry.capture)

(defn select-elem
  "Render select element for the given options. On change, dispatch message
   to change the local entry at the given path. When numeric? is set, coerces
   the value to int."
  [entry options path numeric? put-fn]
  (let [ts (:timestamp entry)
        select-handler (fn [ev]
                         (let [selected (-> ev .-nativeEvent .-target .-value)
                               coerced (if numeric?
                                         (js/parseInt selected)
                                         selected)]
                           (put-fn [:entry/update-local
                                    (assoc-in entry path coerced)])))]
    [:select {:value     (get-in entry path)
              :on-change select-handler}
     [:option {:value ""} ""]
     (for [opt options]
       ^{:key (str ts opt)}
       [:option {:value opt} opt])]))

(defn activity-div
  "In edit mode, allow editing of activities, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (let [activities (:activities cfg)
        ex-levels [1 2 3 4 5 6 7 8 9 10]
        durations (range 0 185 5)]
    (when-let [activity (:activity entry)]
      (if edit-mode?
        [:div
         [:label "Activity:"]
         [select-elem entry activities [:activity :name] false put-fn]
         [:label "Duration:"]
         [select-elem entry durations [:activity :duration-m] true put-fn]
         [:label "Level:"]
         [select-elem entry ex-levels [:activity :exertion-level] true put-fn]]
        [:div "Physical activity: "
         [:strong (:name activity)] " for " [:strong (:duration-m activity)]
         " min, level " [:strong (:exertion-level activity)] "/10."]))))

(defn sleep-div
  "In edit mode, allow editing of sleep data, otherwise show a summary."
  [entry put-fn edit-mode?]
  (let [quality-levels [1 2 3 4 5 6 7 8 9 10]
        interruptions [0 1 2 3 4 5 6 7 8 9 10]
        duration-m (range 0 60 5)
        duration-h (range 0 14)]
    (when (and edit-mode?
               (contains? (:tags entry) "#sleep")
               (not (:sleep entry)))
      (put-fn [:entry/update-local
               (assoc-in entry [:sleep]
                         {:duration-h    0
                          :duration-m    0
                          :quality-level 5
                          :interruptions 0})]))
    (when-let [sleep (:sleep entry)]
      (if edit-mode?
        [:div
         [:label "Sleep Hours:"]
         [select-elem entry duration-h [:sleep :duration-h] true put-fn]
         [:label "Minutes:"]
         [select-elem entry duration-m [:sleep :duration-m] true put-fn]
         [:label "Quality:"]
         [select-elem entry quality-levels [:sleep :quality-level] true put-fn]
         [:label "Interruptions:"]
         [select-elem entry interruptions [:sleep :interruptions] true put-fn]]
        [:div "Sleep: " [:strong (:duration-h sleep)] " h "
         [:strong (:duration-m sleep)] " m, quality "
         [:strong (:quality-level sleep)] "/10. "
         [:strong (:interruptions sleep)] " interruptions."]))))

(defn consumption-div
  "In edit mode, allow editing of consumption, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (let [consumption-types (:consumption-types cfg)
        quantities (range 0 10)]
    (when-let [consumption (:consumption entry)]
      (if edit-mode?
        [:div
         [:label "Consumption:"]
         [select-elem entry consumption-types [:consumption :name] false put-fn]
         [:label "Quantity:"]
         [select-elem entry quantities [:consumption :quantity] true put-fn]]
        [:div "Consumption: "
         [:strong (:name consumption)] ", quantity "
         [:strong (:quantity consumption)]]))))

(defn custom-fields-div
  "In edit mode, allow editing of custom fields, otherwise show a summary."
  [entry cfg put-fn edit-mode?]
  (when-let [custom-fields (:custom-fields cfg)]
    (let [ts (:timestamp entry)
          entry-field-tags (select-keys custom-fields (:tags entry))]
      [:form.custom-fields
       (for [[tag conf] entry-field-tags]
         ^{:key (str "cf" ts tag)}
         [:fieldset
          [:legend tag]
          (for [[k field] (:fields conf)]
            (let [input-cfg (:cfg field)
                  value (get-in entry [:custom-fields tag k])
                  on-change-fn
                  (fn [ev]
                    (let [v (.. ev -target -value)
                          parsed (if (= :number (:type input-cfg))
                                   (when (seq v) (js/parseFloat v))
                                   v)
                          updated (assoc-in entry [:custom-fields tag k] parsed)]
                      (put-fn [:entry/update-local updated])))]
              ^{:key (str "cf" ts tag k)}
              [:span
               [:label (:label field)]
               [:input (merge
                         input-cfg
                         {:read-only (not edit-mode?)
                          :on-change on-change-fn
                          :value     value})]]))])])))

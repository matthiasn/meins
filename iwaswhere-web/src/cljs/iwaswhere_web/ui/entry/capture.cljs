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

(defn girth-div
  "In edit mode, allow editing of body measurement, otherwise show a summary."
  [entry put-fn edit-mode?]
  (let [girth-cm (range 85 100 1)
        girth-mm (range 0 10 1)
        girth (:girth (:measurements entry))]
    (when (and edit-mode?
               (contains? (:tags entry) "#girth")
               (not girth))
      (put-fn [:entry/update-local
               (assoc-in entry [:measurements :girth] {:abdominal-cm 0
                                                       :abdominal-mm 0})]))
    (when girth
      (if edit-mode?
        [:div
         [:label "Abdominal girth:"]
         [select-elem
          entry girth-cm [:measurements :girth :abdominal-cm] true put-fn]
         [:label "cm"]
         [select-elem
          entry girth-mm [:measurements :girth :abdominal-mm] true put-fn]
         [:label "mm"]]
        [:div "Abdominal girth: "
         [:strong (:abdominal-cm girth) "." (:abdominal-mm girth)] " cm. "]))))

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

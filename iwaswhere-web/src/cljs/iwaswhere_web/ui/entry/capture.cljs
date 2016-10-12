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
  "DEPRECATED: use custom fields instead
   In edit mode, allow editing of activities, otherwise show a summary."
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

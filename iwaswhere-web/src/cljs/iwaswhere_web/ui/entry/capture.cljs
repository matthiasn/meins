(ns iwaswhere-web.ui.entry.capture
  (:require [clojure.string :as s]))

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
                  input-type (:type input-cfg)
                  path [:custom-fields tag k]
                  value (get-in entry path)
                  on-change-fn
                  (fn [ev]
                    (let [v (.. ev -target -value)
                          parsed (if (= :number input-type)
                                   (when (seq v) (js/parseFloat v))
                                   v)
                          updated (assoc-in entry path parsed)]
                      (put-fn [:entry/update-local updated])))]
              (when-not value
                (when (and (= input-type :number) edit-mode?)
                  (let [p1 (-> (:md entry) (s/split tag) first)
                        last-n (last (re-seq #"\d+" p1))]
                    (when last-n
                      (let [updated (assoc-in entry path (js/parseFloat last-n))]
                        (put-fn [:entry/update-local updated]))))))
              ^{:key (str "cf" ts tag k)}
              [:span
               [:label (:label field)]
               [:input (merge
                         input-cfg
                         {:read-only (not edit-mode?)
                          :on-change on-change-fn
                          :value     value})]]))])])))

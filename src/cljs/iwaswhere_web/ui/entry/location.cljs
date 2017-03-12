(ns iwaswhere-web.ui.entry.location
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]))

(defn location-details
  [entry put-fn edit-mode?]
  (let [input-fn (fn [entry f k]
                   (fn [ev]
                     (let [n (f (-> ev .-nativeEvent .-target .-value))
                           updated (assoc-in entry [:location k] n)]
                       (put-fn [:entry/update-local updated]))))]
    (fn location-details-render [entry put-fn edit-mode?]
      (when-let [location (:location entry)]
        [:form.task-details
         [:fieldset
          [:legend "Location details"]
          [:div
           [:span " Location name: "]
           (if edit-mode?
             [:input {:type      :text
                      :on-change (input-fn entry identity :name)
                      :value     (:name location)}]
             [:span (:name location)])]
          [:div
           [:span " Radius/m: "]
           (if edit-mode?
             [:input {:type      :number
                      :on-change (input-fn entry js/parseInt :radius)
                      :value     (:radius location)}]
             [:span (:radius location)])]]]))))

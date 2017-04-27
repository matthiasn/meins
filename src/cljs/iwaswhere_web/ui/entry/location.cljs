(ns iwaswhere-web.ui.entry.location
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [iwaswhere-web.helpers :as h]
            [clojure.pprint :as pp]
            [reagent.core :as r]))

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

(defn geonames
  [entry put-fn edit-mode?]
  (let [detail (r/atom false)
        emoji-flags (aget js/window "deps" "emojiFlags")
        toggle-detail (fn [_] (swap! detail not))]
    (fn location-details-render [entry put-fn edit-mode?]
      (when-let [geoname (:geoname entry)]
        (let [admin-4-name (:admin-4-name geoname)
              admin-3-name (:admin-3-name geoname)
              admin-2-name (:admin-2-name geoname)
              country-code (:country-code geoname)
              flag (get (js->clj (.countryCode emoji-flags country-code)) "emoji")]
          [:div {:on-click toggle-detail}
           [:span.geoname (:name geoname)]
           (when (and @detail admin-4-name) [:span.geoname  ", "admin-4-name])
           (when (and @detail admin-3-name) [:span.geoname  ", "admin-3-name])
           (when (and @detail admin-2-name) [:span.geoname  ", " admin-2-name])
           (when-let [admin-1-name (:admin-1-name geoname)]
             [:span.geoname ", " admin-1-name ])
           [:span.flag flag]])))))

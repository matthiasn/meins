(ns iwaswhere-web.ui.entry.flight
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as up]
            [clojure.string :as s]
            [reagent.core :as r]))

(defn flight-view
  [entry put-fn edit-mode? local-cfg]
  (let [url (-> entry :flight :url)
        input-fn (fn [entry]
                   (fn [ev]
                     (let [day (-> ev .-nativeEvent .-target .-value)
                           updated (assoc-in entry [:flight :url] day)]
                       (put-fn [:entry/update-local updated]))))]
    (fn flight-render [entry put-fn edit-mode? local-cfg]
      (when (contains? (:tags entry) "#flight")
        (let [ts (:timestamp entry)]
          [:div.flight
           [:form
            [:fieldset
             [:legend " FlightAware URL: "]
             [:div
              [:input.url {:type     :text
                           :on-input (input-fn entry)
                           :value    url}]]
             [:iframe {:src url}]]]])))))

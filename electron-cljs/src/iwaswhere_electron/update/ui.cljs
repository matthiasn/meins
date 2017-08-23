(ns iwaswhere-electron.update.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.db :as rdb]
            [iwaswhere-electron.update.log :as log]))

;; Subscription Handlers
(reg-sub :current-page (fn [db _] (:current-page db)))


(defn re-frame-ui
  "Main view component"
  [put-fn]
  (let [current-page (subscribe [:current-page])
        check (fn [_]
                (log/info "Check button clicked")
                (put-fn [:update/check]))
        ]
    (fn [put-fn]
      [:div
       [:h1 "Update to latest version of iWasWhere"]
       [:button
        {:on-click check}
        "check"]])))


(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (reagent/render [re-frame-ui put-fn] (.getElementById js/document "app"))
  {:observed rdb/app-db})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

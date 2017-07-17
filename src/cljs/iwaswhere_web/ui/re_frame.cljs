(ns iwaswhere-web.ui.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [iwaswhere-web.ui.menu :as menu]
            [re-frame.core :refer [reg-sub subscribe]]
            [iwaswhere-web.ui.grid :as g]
            [iwaswhere-web.ui.new-entries :as n]
            [iwaswhere-web.ui.stats :as stats]
            [re-frame.db :as rdb]
            [iwaswhere-web.ui.charts.award :as aw]
            [iwaswhere-web.ui.charts.questionnaires :as cq]
            [iwaswhere-web.ui.charts.custom-fields :as cf2]
            [iwaswhere-web.ui.charts.location :as loc]))

;; Subscription Handlers
(reg-sub :custom-field-stats (fn [db _] (:custom-field-stats db)))
(reg-sub :last-update (fn [db _] (:last-update (:query-cfg db))))
(reg-sub :options (fn [db _] (:options db)))
(reg-sub :current-page (fn [db _] (:current-page db)))
(reg-sub :stories (fn [db _] (:stories (:options db))))
(reg-sub :sagas (fn [db _] (:sagas (:options db))))
(reg-sub :busy (fn [db _] (:busy db)))
(reg-sub :query-cfg (fn [db _] (:query-cfg db)))
(reg-sub :widgets (fn [db _] (:widgets (:cfg db))))
(reg-sub :entries-map (fn [db _] (:entries-map db)))
(reg-sub :results (fn [db _] (:results db)))
(reg-sub :new-entries (fn [db _] (:new-entries db)))
(reg-sub :combined-entries (fn [db _] (merge (:entries-map db) (:new-entries db))))
(reg-sub :cfg (fn [db _] (:cfg db)))
(reg-sub :stats (fn [db _] (:stats db)))
(reg-sub :briefings (fn [db _] (:briefings db)))
(reg-sub :started-tasks (fn [db _] (:started-tasks db)))
(reg-sub :waiting-habits (fn [db _] (:waiting-habits db)))
(reg-sub :timing (fn [db _] (:timing db)))
(reg-sub :chart-data (fn [db _]
                       (select-keys db [:pomodoro-stats
                                        :task-stats
                                        :wordcount-stats
                                        :media-stats])))

(defn main-page
  "Main view component"
  [put-fn]
  [:div.flex-container
   [:div.grid
    [:div.wrapper
     [:div.menu
      [menu/menu-view put-fn]]
     [:div.briefing
      [g/tabs-view :briefing put-fn]]
     [:div.left
      [g/tabs-view :left put-fn]]
     [:div.right
      [g/tabs-view :right put-fn]]
     [:div.footer
      [stats/stats-text]]]]
   [n/new-entries-view put-fn]])

(defn charts-page
  "Main view component"
  [put-fn]
  [:div.flex-container
   [:div.charts-grid
    [:div.wrapper
     [aw/award-points put-fn]
     [:div.custom
      [cf2/custom-fields-chart put-fn]]
     [:div.stats
      [stats/stats-view put-fn]]
     [loc/location-chart]
     [:div.footer
      [stats/stats-text]]]]
   [n/new-entries-view put-fn]])

(defn dashboards
  "Dashboard view component"
  [put-fn]
  [:div.flex-container
   [cq/dashboard put-fn]])


(defn re-frame-ui
  "Main view component"
  [put-fn]
  (let [current-page (subscribe [:current-page])]
    (fn [put-fn]
      (let [current-page @current-page]
        (case (:page current-page)
          :dashboards [dashboards put-fn]
          :charts-1 [charts-page put-fn]
          [main-page put-fn])))))


(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (reagent/render [re-frame-ui put-fn]
                  (.getElementById js/document "reframe"))
  {:observed rdb/app-db})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

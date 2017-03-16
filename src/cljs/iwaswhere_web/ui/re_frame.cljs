(ns iwaswhere-web.ui.re-frame
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [cljsjs.react-grid-layout]
            [iwaswhere-web.ui.menu :as menu]
            [re-frame.core :refer [reg-sub]]
            [iwaswhere-web.ui.grid :as g]
            [iwaswhere-web.ui.new-entries :as n]
            [iwaswhere-web.ui.stats :as stats]
            [re-frame.db :as rdb]))

;; Subscription Handlers
(reg-sub :custom-field-stats (fn [db _] (:custom-field-stats db)))
(reg-sub :options (fn [db _] (:options db)))
(reg-sub :stories (fn [db _] (:stories (:options db))))
(reg-sub :books (fn [db _] (:books (:options db))))
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
                                        :activity-stats
                                        :task-stats
                                        :wordcount-stats
                                        :daily-summary-stats
                                        :media-stats])))

(defn re-frame-ui
  "Main view component"
  [put-fn]
  [:div.flex-container
   [menu/menu-view put-fn]
   [g/grid put-fn]
   [stats/stats-text]
   [n/new-entries-view put-fn]])

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

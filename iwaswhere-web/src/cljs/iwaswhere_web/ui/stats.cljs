(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [cljs.pprint :as pp]
            [cljsjs.moment]))

(defn get-pomo-stats
  "Retrieves pomodoro stats for the last n days."
  [put-fn n]
  (fn [_ev]
    (doseq [ds (map #(.format (.subtract (js/moment.) % "d") "YYYY-MM-DD")
                    (range n))]
      (put-fn [:stats/pomo-day-get {:date-string ds}]))))

(defn stats-view
  "Renders stats component."
  [{:keys [observed local put-fn]}]
  (let [store-snapshot @observed
        pomodoro-stats (:pomodoro-stats store-snapshot)]
    [:div
     [:div {:on-click (get-pomo-stats put-fn 14)} "get pomodoro stats"]
     (for [[ds ps] pomodoro-stats]
       ^{:key ds}
       [:div ds " completed: " (:completed ps) " time: " (:total-time ps)])]))

(defn init-fn
  ""
  [{:keys [local observed put-fn]}]
  (let []))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :init-fn init-fn
              :view-fn stats-view
              :dom-id  "stats"}))

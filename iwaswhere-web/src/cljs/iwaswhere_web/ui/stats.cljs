(ns iwaswhere-web.ui.stats
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [cljsjs.moment]
            [iwaswhere-web.ui.pomodoro :as p]))

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
        pomodoro-stats (:pomodoro-stats store-snapshot)
        cfg (:cfg store-snapshot)
        entries-map (:entries-map store-snapshot)
        entries (map (fn [ts] (get entries-map ts)) (:entries store-snapshot))]
    [:div.stats
     [:button {:on-click (get-pomo-stats put-fn 14)} "get pomodoro stats"]
     (for [[ds ps] pomodoro-stats]
       ^{:key ds}
       [:div ds " total: " (:total ps) " completed: " (:completed ps)
        " started: " (:started ps) " time: " (:total-time ps)])
     (when-let [stats (:stats store-snapshot)]
       [:div (:entry-count stats) " entries, " (:node-count stats) " nodes, "
        (:edge-count stats) " edges, " (count (:hashtags cfg)) " hashtags, "
        (count (:mentions cfg)) " people"])
     (when-let [ms (get-in store-snapshot [:timing :query])]
       [:div.stats
        (str "Query with " (count entries)
             " results completed in " ms ", RTT "
             (get-in store-snapshot [:timing :rtt]) " ms")])]))

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

(ns iwaswhere-web.ui.stats
  (:require [iwaswhere-web.ui.charts.tasks :as ct]
            [iwaswhere-web.ui.charts.custom-fields :as cf]
            [iwaswhere-web.ui.charts.wordcount :as wc]
            [iwaswhere-web.ui.charts.location :as loc]
            [iwaswhere-web.ui.charts.time.durations :as cd]
            [iwaswhere-web.ui.charts.media :as m]
            [iwaswhere-web.helpers :as h]
            [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pp]
            [iwaswhere-web.ui.charts.award :as aw]))

(defn stats-text
  "Renders stats text component."
  []
  (let [stats (subscribe [:stats])
        options (subscribe [:options])
        cfg (subscribe [:cfg])
        timing (subscribe [:timing])]
    (fn stats-text-render []
      [:div.stats-string
       (when stats
         [:div
          (:entry-count @stats) " entries, "
          (count (:hashtags @options)) " tags, "
          (count (:mentions @options)) " people, "
          (Math/floor (:hours-logged @stats)) " hours, "
          (:word-count @stats) " words, "
          (:open-tasks-cnt @stats) " open tasks, "
          (:backlog-cnt @stats) " backlog, "
          (:completed-cnt @stats) " done, "
          (:closed-cnt @stats) " closed, "
          (:import-cnt @stats) " #import. PID: " (:pid @cfg)
          (when-let [ms (:query @timing)]
            (str ". Query with " (:count @timing)
                 " results: " ms ", RTT "
                 (:rtt @timing) " ms"))])])))

(defn stats-view
  "Renders stats component."
  [put-fn]
  [:div.stats
   [:div.charts
    [cd/durations-table 200 5 put-fn]
    [ct/tasks-chart 250 put-fn]
    [wc/wordcount-chart 150 put-fn 1000]
    [m/media-chart 150 put-fn]]])

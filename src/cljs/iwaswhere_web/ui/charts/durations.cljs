(ns iwaswhere-web.ui.charts.durations
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.pprint :as pp]
            [iwaswhere-web.charts.data :as cd]))

(defn ts-bars
  "Renders group with rects for all stories of the particular day."
  [day-stats local idx chart-h y-scale put-fn]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :y] total))))]
    (fn [day-stats local idx chart-h y-scale put-fn]
      (let [day (js/moment (:date-string day-stats))
            day-millis (.valueOf day)
            mouse-enter-fn (cc/mouse-enter-fn local day-stats)
            mouse-leave-fn (cc/mouse-leave-fn local day-stats)
            stories @stories
            time-by-ts (:time-by-ts day-stats)
            time-by-h (map (fn [[ts v]]
                             (let [h (/ (- ts day) 1000 60 60)]
                               [h v])) time-by-ts)]
        [:g
         {:on-mouse-enter mouse-enter-fn
          :on-mouse-leave mouse-leave-fn}
         (for [[hh {:keys [story-name summed manual]}] time-by-h]
           (let [h (* y-scale summed)
                 y (* y-scale (+ hh 2) 60 60)
                 y (if (pos? manual) (- y h) y)]
             ^{:key (str story-name hh)}
             [:rect {:fill           (cc/item-color story-name)
                     :on-mouse-enter #(prn story-name hh summed)
                     :x              (* 30 idx)
                     :y              y
                     :width          26
                     :height         h}]))]))))

(defn bars-by-ts
  "Renders chart with daily recorded times, split up by story."
  [indexed local chart-h y-scale put-fn]
  [:svg
   {:viewBox (str "0 0 600 " chart-h)}
   [:g
    [cc/chart-title "24h"]
    (for [h (range 28)]
      (let [y (* chart-h (/ h 28))
            stroke-w (if (zero? (mod (- h 2) 6)) 2 1)]
        ^{:key h}
        [:line {:x1 0 :x2 600 :y1 y :y2 y :stroke-width stroke-w :stroke "#999"}]))
    [:g
     (for [[idx v] indexed]
       (let [h (* y-scale (:total-time v))
             mouse-enter-fn (cc/mouse-enter-fn local v)
             mouse-leave-fn (cc/mouse-leave-fn local v)]
         ^{:key (str idx)}
         [ts-bars v local idx chart-h y-scale put-fn]))]]])

(defn day-bars
  "Renders group with rects for all stories of the particular day."
  [day-stats local idx chart-h y-scale put-fn]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :y] total))))]
    (fn [day-stats local idx chart-h y-scale put-fn]
      (let [mouse-enter-fn (cc/mouse-enter-fn local day-stats)
            mouse-leave-fn (cc/mouse-leave-fn local day-stats)
            stories @stories
            time-by-story (sort-by #(str (first %)) (:time-by-story day-stats))
            stacked (reduce stacked-reducer {} time-by-story)
            time-by-story2 (reverse (sort-by #(str (first %)) (:items stacked)))
            weekend? (cc/weekend? (:date-string day-stats))]
        [:g
         {:on-mouse-enter mouse-enter-fn
          :on-mouse-leave mouse-leave-fn}
         (for [[story {:keys [y v]}] time-by-story2]
           (let [h (* y-scale v)
                 y (- chart-h (+ h (* y-scale y)))
                 story-name (or (:story-name (get stories story)) "No story")]
             ^{:key (str story)}
             [:rect {:on-click (cc/open-day-fn v put-fn)
                     :fill     (cc/item-color story-name)
                     :x        (* 30 idx)
                     :y        y
                     :width    26
                     :height   h}]))
         (when weekend?
           [:rect {:fill    :white
                   :x       (* 30 idx)
                   :y       0
                   :opacity 0.5
                   :width   26
                   :height  chart-h}])]))))

(defn bars-by-story
  "Renders chart with daily recorded times, split up by story."
  [indexed local chart-h y-scale put-fn]
  [:svg
   {:viewBox (str "0 0 600 " chart-h)}
   [:g
    [:g
     (for [[idx v] indexed]
       (let [h (* y-scale (:total-time v))
             mouse-enter-fn (cc/mouse-enter-fn local v)
             mouse-leave-fn (cc/mouse-leave-fn local v)]
         ^{:key (str idx)}
         [day-bars v local idx chart-h y-scale put-fn]))]
    [cc/chart-title "by story"]]])

;; TODO: either DRY up or rethink
(defn day-bars-by-saga
  "Renders group with rects for all stories of the particular day."
  [day-stats local idx chart-h y-scale put-fn]
  (let [options (subscribe [:options])
        sagas (reaction (:sagas @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :y] total))))]
    (fn [day-stats local idx chart-h y-scale put-fn]
      (let [mouse-enter-fn (cc/mouse-enter-fn local day-stats)
            mouse-leave-fn (cc/mouse-leave-fn local day-stats)
            sagas @sagas
            time-by-saga (sort-by #(str (first %)) (:time-by-saga day-stats))
            stacked (reduce stacked-reducer {} time-by-saga)
            time-by-saga (reverse (sort-by #(str (first %)) (:items stacked)))
            weekend? (cc/weekend? (:date-string day-stats))]
        [:g
         {:on-mouse-enter mouse-enter-fn
          :on-mouse-leave mouse-leave-fn}
         (for [[saga {:keys [y v]}] time-by-saga]
           (let [h (* y-scale v)
                 y (- chart-h (+ h (* y-scale y)))
                 saga-name (or (:saga-name (get sagas saga)) "No saga")
                 weekday? (not (cc/weekend? (:date-string day-stats)))]
             ^{:key (str saga)}
             [:rect {:on-click (cc/open-day-fn v put-fn)
                     :fill     (cc/item-color saga-name)
                     :x        (* 30 idx)
                     :y        y
                     :width    26
                     :height   h}]))
         (when weekend?
           [:rect {:fill    :white
                   :x       (* 30 idx)
                   :y       0
                   :opacity 0.5
                   :width   26
                   :height  chart-h}])]))))

(defn bars-by-saga
  "Renders chart with daily recorded times, split up by story."
  [indexed local chart-h y-scale put-fn]
  [:svg
   {:viewBox (str "0 0 600 " chart-h)}
   [:g
    [:g
     (for [[idx v] indexed]
       (let [h (* y-scale (:total-time v))
             mouse-enter-fn (cc/mouse-enter-fn local v)
             mouse-leave-fn (cc/mouse-leave-fn local v)]
         ^{:key (str idx)}
         [day-bars-by-saga v local idx chart-h y-scale put-fn]))]
    [cc/chart-title "by saga"]]])

(defn time-by-stories-list
  "Render list of times spent on individual stories, plus the total."
  [day-stats]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        sagas (reaction (:sagas @options))]
    (fn [day-stats]
      (let [stories @stories
            sagas @sagas
            dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)]
        (when date
          [:div.story-time
           [:table
            [:tbody
             [:tr [:th ""] [:th "saga"] [:th "total"]]
             (for [[saga v] (:time-by-saga day-stats)]
               (let [saga-name (or (:saga-name (get sagas saga)) "none")
                     color (cc/item-color saga-name)]
                 ^{:key saga}
                 [:tr
                  [:td [:div.legend {:style {:background-color color}}]]
                  [:td [:strong saga-name]]
                  [:td.time (u/duration-string v)]]))]]
           [:table
            [:tbody
             [:tr [:th ""] [:th "story"] [:th "total"]]
             (for [[story v] (:time-by-story day-stats)]
               (let [story-name (or (:story-name (get stories story)) "none")
                     color (cc/item-color story-name)]
                 ^{:key story}
                 [:tr
                  [:td [:div.legend {:style {:background-color color}}]]
                  [:td [:strong story-name]]
                  [:td.time (u/duration-string v)]]))]]])))))

(defn award-points
  "Simple view for points awarded."
  []
  (let [stats (subscribe [:stats])]
    (fn []
      [:div "Award points: " [:strong (:award-points @stats)]])))

(defn durations-bar-chart
  [stats chart-h title y-scale put-fn]
  (let [local (rc/atom {})
        idx-fn (fn [idx [k v]] [idx v])
        sagas (subscribe [:sagas])
        chart-data (subscribe [:chart-data])]
    (fn [stats chart-h title y-scale put-fn]
      (let [sagas @sagas
            indexed (map-indexed idx-fn stats)
            indexed-20 (map-indexed idx-fn (take-last 20 stats))
            day-stats (or (:mouse-over @local) (second (last stats)))
            past-7-days (cd/past-7-days stats :time-by-saga)
            dur (u/duration-string (:total-time day-stats))
            fmt-date (.format (js/moment (:date-string day-stats)) "ddd YY-MM-DD")]
        [:div
         [:div.times-by-day
          [award-points]
          [:div [cc/horizontal-bar sagas :saga-name past-7-days 0.001]]
          [:div
           "Past seven days: "
           [:strong (u/duration-string (apply + (map second past-7-days)))]]
          [:div.story-time
           [:table
            [:tbody
             [:tr [:th ""] [:th "saga"] [:th "total"]]
             (for [[saga v] past-7-days]
               (let [saga-name (or (:saga-name (get sagas saga)) "none")
                     color (cc/item-color saga-name)]
                 ^{:key saga}
                 [:tr
                  [:td [:div.legend {:style {:background-color color}}]]
                  [:td [:strong saga-name]]
                  [:td.time (u/duration-string v)]]))]]]]
         [bars-by-story indexed-20 local chart-h 0.0035 put-fn]
         [bars-by-saga indexed-20 local chart-h 0.0035 put-fn]
         [bars-by-ts indexed-20 local 443.5 0.0044 put-fn]
         [:div.times-by-day
          [:div [:time fmt-date]
           " - " [:strong dur] " in " (:total day-stats) " entries."]
          [time-by-stories-list day-stats]]]))))

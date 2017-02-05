(ns iwaswhere-web.ui.charts.pomodoros
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.pprint :as pp]))

(defn bars
  [indexed local k chart-h y-scale put-fn]
  [:g
   (for [[idx v] indexed]
     (let [h (* y-scale (k v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       ^{:key (str "pbar" k idx)}
       [:rect {:class          (cc/weekend-class (name k) v)
               :on-click       (cc/open-day-fn v put-fn)
               :x              (* 10 idx)
               :y              (- chart-h h)
               :width          9
               :height         h
               :on-mouse-enter mouse-enter-fn
               :on-mouse-leave mouse-leave-fn}]))])

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
            time-by-story2 (reverse (sort-by #(str (first %)) (:items stacked)))]
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
                     :height   h}]))]))))

(defn bars-by-story
  "Renders chart with daily recorded times, split up by story."
  [indexed local chart-h y-scale put-fn]
  [:g
   (for [[idx v] indexed]
     (let [h (* y-scale (:total-time v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       ^{:key (str idx)}
       [day-bars v local idx chart-h y-scale put-fn]))])

;; TODO: either DRY up or rethink
(defn day-bars-by-book
  "Renders group with rects for all stories of the particular day."
  [day-stats local idx chart-h y-scale put-fn]
  (let [options (subscribe [:options])
        books (reaction (:books @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :y] total))))]
    (fn [day-stats local idx chart-h y-scale put-fn]
      (let [mouse-enter-fn (cc/mouse-enter-fn local day-stats)
            mouse-leave-fn (cc/mouse-leave-fn local day-stats)
            books @books
            time-by-book (sort-by #(str (first %)) (:time-by-book day-stats))
            stacked (reduce stacked-reducer {} time-by-book)
            time-by-book (reverse (sort-by #(str (first %)) (:items stacked)))]
        [:g
         {:on-mouse-enter mouse-enter-fn
          :on-mouse-leave mouse-leave-fn}
         (for [[book {:keys [y v]}] time-by-book]
           (let [h (* y-scale v)
                 y (- chart-h (+ h (* y-scale y)))
                 book-name (or (:book-name (get books book)) "No book")]
             ^{:key (str book)}
             [:rect {:on-click (cc/open-day-fn v put-fn)
                     :fill     (cc/item-color book-name)
                     :x        (* 30 idx)
                     :y        y
                     :width    26
                     :height   h}]))]))))

(defn bars-by-book
  "Renders chart with daily recorded times, split up by story."
  [indexed local chart-h y-scale put-fn]
  [:g
   (for [[idx v] indexed]
     (let [h (* y-scale (:total-time v))
           mouse-enter-fn (cc/mouse-enter-fn local v)
           mouse-leave-fn (cc/mouse-leave-fn local v)]
       ^{:key (str idx)}
       [day-bars-by-book v local idx chart-h y-scale put-fn]))])

(defn time-by-stories-list
  "Render list of times spent on individual stories, plus the total."
  [day-stats]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        books (reaction (:books @options))]
    (fn [day-stats]
      (let [stories @stories
            books @books
            dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)]
        (when date
          [:div.story-time
           [:div "Logged: " [:strong dur] " in " (:total day-stats) " entries."]
           [:hr]
           (for [[book v] (:time-by-book day-stats)]
             (let [book-name (or (:book-name (get books book)) "No book")]
               ^{:key book}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color book-name)}}]
                [:strong book-name] ": " (u/duration-string v)]))
           [:hr]
           (for [[story v] (:time-by-story day-stats)]
             (let [story-name (or (:story-name (get stories story)) "No story")]
               ^{:key story}
               [:div
                [:span.legend
                 {:style {:background-color (cc/item-color story-name)}}]
                [:strong story-name] ": " (u/duration-string v)]))])))))

(defn pomodoro-bar-chart
  [pomodoro-stats chart-h title y-scale put-fn]
  (let [local (rc/atom {})
        idx-fn (fn [idx [k v]] [idx v])
        chart-data (subscribe [:chart-data])]
    (fn [pomodoro-stats chart-h title y-scale put-fn]
      (let [indexed (map-indexed idx-fn pomodoro-stats)
            indexed-20 (map-indexed idx-fn (take-last 20 pomodoro-stats))
            day-stats (or (:mouse-over @local) (second (last pomodoro-stats)))]
        [:div
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [cc/chart-title "Time tracked"]
           [bars-by-story indexed-20 local chart-h 0.0045 put-fn]]]
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [cc/chart-title "Time tracked"]
           [bars-by-book indexed-20 local chart-h 0.0045 put-fn]]]
         [:div.times-by-day
          [:time (:date-string day-stats)]

          [time-by-stories-list day-stats]]
         [:svg
          {:viewBox (str "0 0 600 " chart-h)}
          [:g
           [cc/chart-title title]
           [cc/bg-bars indexed local chart-h :pomodoro]
           [bars indexed local :total chart-h y-scale put-fn]
           [bars indexed local :completed chart-h y-scale put-fn]
           [cc/path "M 0 50 l 600 0 z"]
           [cc/path "M 0 100 l 600 0 z"]]]]))))

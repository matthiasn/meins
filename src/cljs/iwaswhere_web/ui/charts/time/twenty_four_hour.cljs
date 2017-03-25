(ns iwaswhere-web.ui.charts.time.twenty-four-hour
  (:require [reagent.core :as rc]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [clojure.pprint :as pp]
            [iwaswhere-web.charts.data :as cd]))

(defn ts-bars
  "Renders group with rects for all stories of the particular day."
  [day-stats local item-name-k idx chart-h y-scale put-fn]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        sagas (reaction (:sagas @options))
        stacked-reducer (fn [acc [k v]]
                          (let [total (get acc :total 0)]
                            (-> acc
                                (assoc-in [:total] (+ total v))
                                (assoc-in [:items k :v] v)
                                (assoc-in [:items k :y] total))))]
    (fn [day-stats local item-name-k idx chart-h y-scale put-fn]
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
         (for [[hh {:keys [summed manual] :as data}] time-by-h]
           (let [h (* y-scale summed)
                 y (* y-scale (+ hh 2) 60 60)
                 item-name (item-name-k data)
                 y (if (pos? manual) (- y h) y)]
             ^{:key (str item-name hh)}
             [:rect {:fill           (cc/item-color item-name)
                     :on-mouse-enter #(prn item-name hh summed)
                     :x              (* 30 idx)
                     :y              y
                     :width          26
                     :height         h}]))]))))

(defn earlybird-nightowl
  "Renders chart with daily recorded times, split up by story."
  [indexed local item-name-k chart-h y-scale put-fn]
  [:svg
   {:viewBox (str "0 0 420 " chart-h)}
   [:g
    [cc/chart-title "24h" 210]
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
         [ts-bars v local item-name-k idx chart-h y-scale put-fn]))]]])

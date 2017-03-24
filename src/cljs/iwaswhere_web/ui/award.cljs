(ns iwaswhere-web.ui.award
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.helpers :as h]))

(defn award-points
  "Simple view for points awarded."
  [put-fn]
  (let [stats (subscribe [:stats])]
    (fn [put-fn]
      [:div.award
       [:div.points (:award-points @stats)]
       "Award points"])))
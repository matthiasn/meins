(ns iwaswhere-web.ui.calendar
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pp]
            [reagent.core :as r]
            [iwaswhere-web.utils.parse :as up]))

(defn calendar-view
  "Renders calendar component."
  [put-fn]
  (let [calendar (r/adapt-react-class (aget js/window "deps" "Calendar" "default"))
        select-fn (fn [dt]
                    (let [fmt (.format dt "YYYY-MM-DD")
                          q (up/parse-search (str "briefing:" fmt))]
                      (put-fn [:search/add {:tab-group :left :query q}])))]
    (fn stats-view-render [put-fn]
      (let []
        [:div.calendar
         [calendar {:events []
                    :on-select select-fn}]]))))

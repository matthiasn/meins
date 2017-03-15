(ns iwaswhere-web.ui.calendar
  (:require [cljsjs.moment]
            [re-frame.core :refer [subscribe]]
            [cljs.pprint :as pp]
            [reagent.core :as r]
            [iwaswhere-web.utils.parse :as up]
            [iwaswhere-web.utils.parse :as p]
            [iwaswhere-web.helpers :as h]))

(defn calendar-view
  "Renders calendar component."
  [put-fn]
  (let [calendar (r/adapt-react-class (aget js/window "deps" "FullCalendar" "default"))
        briefings (subscribe [:briefings])
        cfg (subscribe [:cfg])
        select-fn (fn [dt]
                    (let [fmt (.format dt "YYYY-MM-DD")
                          q (up/parse-search (str "briefing:" fmt))]
                      (when-not (get @briefings fmt)
                        (let [weekday (.format dt "dddd")
                              md (str "## " weekday "'s #briefing")
                              new-entry (merge
                                          (p/parse-entry md)
                                          {:briefing     {:day fmt}
                                           :linked-story (:briefing-story @cfg)})
                              new-entry-fn (h/new-entry-fn put-fn new-entry nil)]
                          (new-entry-fn)))
                      (put-fn [:search/add {:tab-group :left :query q}])))
        cell-render (r/reactify-component
                      (fn [props]
                        (let [m (js/moment (:_d props))
                              fmt (.format m "YYYY-MM-DD")
                              cls (when (get @briefings fmt) "briefing-exists")]
                          [:div.rc-calendar-date
                           {:class cls}
                           (.date m)])))]
    (fn stats-view-render [put-fn]
      (let [briefings @briefings]
        [:div.calendar
         [calendar {:events      []
                    :on-select   select-fn
                    :foo         "bar"
                    :briefings   briefings
                    :cell-render cell-render}]]))))

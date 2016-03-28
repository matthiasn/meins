(ns iwaswhere-web.journal
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.leaflet :as l]
            [iwaswhere-web.helpers :as h]
            [iwaswhere-web.markdown :as m]
            [cljsjs.moment]))

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed]}]
  (let [store-snapshot @observed]
    [:div:div.l-box-lrg.pure-g
     [:div.pure-u-1
      [:hr]
      (let [entries (reverse (:entries store-snapshot))]
        (for [entry (take 50 (filter (h/entries-filter-fn (:new-entry store-snapshot)) entries))]
          ^{:key (:timestamp entry)}
          [:div.entry
           [:span.timestamp (.format (js/moment (:timestamp entry)) "MMMM Do YYYY, h:mm:ss a")]
           (m/markdown-render entry)
           (when-let [lat (:latitude entry)]
             [l/leaflet-component {:id  (str "map" (:timestamp entry))
                                   :lat lat
                                   :lon (:longitude entry)}])
           [:hr]]))]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :initial-state {}
              :view-fn journal-view
              :dom-id  "journal"}))

(ns iwaswhere-web.journal
  (:require [markdown.core :as md]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.leaflet :as l]
            [cljsjs.moment]))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous here since
  application is only running locally, so in doubt we could only harm ourselves."
  [md-string]
  [:div {:dangerouslySetInnerHTML {:__html (-> md-string (md/md->html md-string))}}])

(defn journal-view
  "Renders journal div, one entry per item, with map if geo data exists in the entry."
  [{:keys [observed]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:hr]
    (for [entry (reverse (:entries @observed))]
      ^{:key (:timestamp entry)}
      [:div.entry
       [:span.timestamp (.format (js/moment (:timestamp entry)) "MMMM Do YYYY, h:mm:ss a")]
       (markdown-render (:md entry))
       (when-let [lat (:latitude entry)]
         [l/leaflet-component {:id  (str "map" (:timestamp entry))
                               :lat lat
                               :lon (:longitude entry)}])
       [:hr]])]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

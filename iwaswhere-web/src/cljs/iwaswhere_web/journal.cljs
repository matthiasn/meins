(ns iwaswhere-web.journal
  (:require [markdown.core :as md]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :as h]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [reagent.core :as rc]
            [cljsjs.moment]
            [cljsjs.leaflet]
            [cljs.pprint :as pp]))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous here since
  application is only running locally, so in doubt we could only harm ourselves."
  [md-string]
  [:div {:dangerouslySetInnerHTML {:__html (-> md-string (md/md->html md-string))}}])

(defn leaflet-did-mount
  "Function using the did-mount lifecycle method. Uses :id from props map to determine
  id to render into, and then sets the view to latitude and longitude, also from the
  props map."
  [props]
  (fn []
    (let [{:keys [lat lon]} props
          map (.setView (.map js/L (:id props) (clj->js {:scrollWheelZoom false})) #js [lat lon] 13)]
      (.addTo (.tileLayer js/L "http://{s}.tile.osm.org/{z}/{x}/{y}.png" (clj->js {:maxZoom 18})) map)
      (.addTo (.marker js/L #js [lat lon]) map))))

(defn leaflet-component
  "Creates a leaflet map reagent class. The reagent-render function only creates a div with
  the id from the props map. The component-did-mount then fills the map div with life, with
  the latitude and longitude from props.
  TODO: disable zoom, other options for map; change map when data changes; marker"
  [props]
  (set! (.-imagePath js/L.Icon.Default) "bower_components/leaflet/dist/images/")
  (rc/create-class {:component-did-mount (leaflet-did-mount props)
                    :reagent-render      (fn [props] [:div.map {:id (:id props)}])}))

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
         [leaflet-component {:id  (str "map" (:timestamp entry))
                             :lat lat
                             :lon (:longitude entry)}])
       [:hr]])]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

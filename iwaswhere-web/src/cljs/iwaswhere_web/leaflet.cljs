(ns iwaswhere-web.leaflet
  (:require [reagent.core :as rc]
            [cljsjs.leaflet]))

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

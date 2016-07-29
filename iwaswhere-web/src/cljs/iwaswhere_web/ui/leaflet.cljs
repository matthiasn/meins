(ns iwaswhere-web.ui.leaflet
  (:require [reagent.core :as rc]
            [cljsjs.leaflet]))

(defn leaflet-did-mount
  "Function using the did-mount lifecycle method. Uses :id from props map to
   determine id to render into, and then sets the view to latitude and
   longitude, also from the props map."
  [props]
  (fn []
    (let [{:keys [lat lon]} props
          map-cfg (clj->js {:scrollWheelZoom false})
          map (.setView (.map js/L (:id props) map-cfg) #js [lat lon] 13)
          tiles-url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"]
      (.addTo (.tileLayer js/L tiles-url (clj->js {:maxZoom 18})) map)
      (.addTo (.marker js/L #js [lat lon]) map))))

(defn leaflet-component
  "Creates a leaflet map reagent class. The reagent-render function only creates
   a div with the id from the props map. The component-did-mount then fills the
   map div with life, with the latitude and longitude from props.
   TODO: disable zoom, other options for map; change map when data changes"
  [props]
  (set! (.-imagePath js/L.Icon.Default) "/webjars/leaflet/0.7.7/dist/images/")
  (rc/create-class
    {:component-did-mount (leaflet-did-mount props)
     :reagent-render      (fn [props] [:div.map {:id (:id props)}])}))

(defn leaflet-map
  "Helper for showing map when exists and desired."
  [entry show?]
  (when show?
    (when-let [lat (:latitude entry)]
      [leaflet-component {:id  (str "map" (:timestamp entry))
                          :lat lat
                          :lon (:longitude entry)}])))

(ns iwaswhere-web.new-entry
  (:require [markdown.core :as md]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :as h]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.string :as s]
            [cljsjs.moment]
            [cljsjs.leaflet]
            [cljs.pprint :as pp]))

(defn w-geolocation
  [data pos]
  (let [coords (.-coords pos)
        latitude (.-latitude coords)
        longitude (.-longitude coords)]
    (merge data {:latitude  latitude
                 :longitude longitude
                 :timestamp (.-timestamp pos)})))

(defn send-w-geolocation
  [data put-fn]
  (let [geo (.-geolocation js/navigator)]
    (.getCurrentPosition geo (fn [pos]
                               (let [w-geoloc (w-geolocation data pos)]
                                 (pp/pprint w-geoloc)
                                 (put-fn [:geo-entry/persist w-geoloc]))))))

(defn pan-to
  [map]
  (let [geo (.-geolocation js/navigator)]
    (.getCurrentPosition geo (fn [pos]
                               (let [coords (.-coords pos)
                                     latitude (.-latitude coords)
                                     longitude (.-longitude coords)]
                                 (.panTo map (new js/L.LatLng latitude longitude))
                                 (.addTo (.marker js/L #js [latitude longitude]) map))))))

(defn new-entry-view
  "Renders Journal div"
  [{:keys [observed local put-fn]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div [:textarea#input
           {:type      "text"
            ; TODO: occasionally store content into localstorage
            :on-change #(swap! local assoc-in [:input] (.. % -target -value))
            :style     {:height (str (+ 6 (count (s/split-lines (:input @local)))) "em")}}]]
    [:div [:button {:on-click (fn [_ev]
                                (send-w-geolocation {} put-fn)
                                (pan-to (:map @local))
                                (put-fn [:text-entry/persist {:md        (.-value (h/by-id "input"))
                                                              :timestamp (st/now)}]))} "save"]
     [:button {:on-click (fn [_ev] (pan-to (:map @local)))} "update map"]]]])

(defn init
  []
  (let [map (.map js/L "map" (clj->js {:scrollWheelZoom false}))
        map (.setView map #js [0 0] 13)]
    (.addTo (.tileLayer js/L "http://{s}.tile.osm.org/{z}/{x}/{y}.png" (clj->js {:maxZoom 18})) map)
    (pan-to map)
    (set! (.-imagePath js/L.Icon.Default) "bower_components/leaflet/dist/images/")
{:map map}))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :initial-state (init)
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

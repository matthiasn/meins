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

(defn new-entry-view
  "Renders Journal div"
  [{:keys [local put-fn]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div [:textarea#input
           {:type      "text"
            ; TODO: occasionally store content into localstorage
            :on-change #(swap! local assoc-in [:input] (.. % -target -value))
            :style     {:height (str (+ 6 (count (s/split-lines (:input @local)))) "em")}}]]
    [:div [:button {:on-click (fn [_ev]
                                (send-w-geolocation {} put-fn)
                                (put-fn [:text-entry/persist {:md        (.-value (h/by-id "input"))
                                                              :timestamp (st/now)}]))} "save"]]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

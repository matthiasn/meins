(ns iwaswhere-web.journal
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

(defn markdown-render
  ""
  [md-string]
  [:div {:dangerouslySetInnerHTML {:__html (-> md-string (md/md->html md-string))}}])

(defn journal-view
  "Renders Journal div"
  [{:keys [observed local put-fn]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div [:textarea#input
           {:type      "text"
            ; TODO: occasionally store content into localstorage, instead of on every keystroke
            ;:on-change #(swap! local assoc-in [:input] (.. % -target -value))
            :style     {:height (str (+ 6 (count (s/split-lines (:input @local)))) "em")}}]]
    [:div [:button {:on-click (fn [_ev]
                                ;(.log js/console (.-value (h/by-id "input")))
                                (send-w-geolocation {} put-fn)
                                (put-fn [:text-entry/persist {:md        (.-value (h/by-id "input"))
                                                              :timestamp (st/now)}]))} "save"]]]
   [:div.pure-u-1
    [:hr]
    (for [entry (reverse (:entries @observed))]
      ^{:key (:timestamp entry)}
      [:div.entry
       [:span.timestamp (.format (js/moment (:timestamp entry)) "MMMM Do YYYY, h:mm:ss a")]
       (markdown-render (:md entry))
       (when-let [lat (:latitude entry)]
         [:div
          [:span "lat: " lat " lon: " (:longitude entry)]
          [:div.map {:id (str "map" (:timestamp entry))}]])
       [:hr]])]])

(defn home-did-mount []
  (let [map (.setView (.map js/L "map") #js [53.565221099999995 9.9832887] 13)]
    ;; NEED TO REPLACE FIXME with your mapID!
    (.addTo (.tileLayer js/L "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                        (clj->js {:attribution "Map data &copy; [...]"
                                  :maxZoom     18}))
            map)))

(home-did-mount)

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn journal-view
              :dom-id  "journal"}))

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

(defn parse-entry
  "Parses entry for hastags and mentions."
  [text]
  (let [tags (into [] (re-seq (js/RegExp. "(?!^)#[\\w\\-]+" "m") text))
        mentions (into [] (re-seq (js/RegExp. "@\\w+" "m") text))]
    {:md        text
     :tags      tags
     :mentions  mentions
     :timestamp (st/now)}))

(defn new-entry-view
  "Renders Journal div."
  [{:keys [local put-fn]}]
  [:div:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div [:textarea#new-entry
           {:type      "text"
            ; TODO: occasionally store content into localstorage
            :on-change #(reset! local (parse-entry (.. % -target -value)))}]]
    #_(h/pp-div @local)
    [:div.entry-footer
     [:button {:on-click (fn [_ev]
                           (send-w-geolocation {} put-fn)
                           (put-fn [:text-entry/persist @local]))} "save"]
     (for [hashtag (:tags @local)]
       ^{:key (str "tag-" hashtag)}
       [:span.hashtag hashtag])]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

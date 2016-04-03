(ns iwaswhere-web.new-entry
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox.component :as st]
            [cljsjs.moment]
            [cljsjs.leaflet]
            [cljs.pprint :as pp]))

(defn send-w-geolocation
  "Calls geolocation, sends entry enriched by geo information inside the
  callback function"
  [data put-fn]
  (.getCurrentPosition
    (.-geolocation js/navigator)
    (fn [pos]
      (let [coords (.-coords pos)]
        (put-fn [:geo-entry/persist
                 (merge data {:latitude  (.-latitude coords)
                              :longitude (.-longitude coords)})])))))

(defn parse-entry
  "Parses entry for hastags and mentions. Either can consist of any of the word characters, dashes
  and unicode characters that for example comprise German 'Umlaute'."
  [text]
  (let [tags (into [] (re-seq (js/RegExp. "(?!^)#[\\w\\-\\u00C0-\\u017F]+" "m") text))
        mentions (into [] (set (re-seq (js/RegExp. "@[\\w\\-\\u00C0-\\u017F]+" "m") text)))]
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
            :on-change (fn [ev]
                         (reset! local (parse-entry (.. ev -target -value)))
                         (put-fn [:text-entry/save @local]))}]]
    #_(h/pp-div @local)
    [:div.entry-footer
     [:button.pure-button.pure-button-primary
      {:on-click #(let [entry @local]
                   (put-fn [:text-entry/persist entry])
                   (send-w-geolocation entry put-fn))}
      "save"]
     [:button.pure-button {:on-click #(put-fn [:import/photos])} [:span.fa.fa-camera-retro] " import"]
     (for [hashtag (:tags @local)]
       ^{:key (str "tag-" hashtag)}
       [:span.hashtag hashtag])]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

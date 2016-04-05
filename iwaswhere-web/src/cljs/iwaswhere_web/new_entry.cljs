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
  (let [tags (set (re-seq (js/RegExp. "(?!^)#[\\w\\-\\u00C0-\\u017F]+" "m") text))
        mentions (set (re-seq (js/RegExp. "@[\\w\\-\\u00C0-\\u017F]+" "m") text))]
    {:md        text
     :tags      tags
     :mentions  mentions
     :timestamp (st/now)}))

(defn new-entry-view
  "Renders Journal div."
  [{:keys [local put-fn]}]
  [:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div [:textarea#new-entry-textbox
           {:type      "text"
            ; TODO: occasionally store content into localstorage
            :on-change (fn [ev]
                         (let [prev-tags (or (:prev-tags @local) #{})
                               new-state (parse-entry (.. ev -target -value))
                               new-tags (:tags new-state)]
                           (swap! local assoc-in [:entry] new-state)
                           (put-fn [:text-entry/save new-state])
                           (when (not= prev-tags new-tags)
                             (put-fn [:state/get {:tags new-tags}])
                             (swap! local assoc-in [:prev-tags] new-tags))))}]]
    #_(h/pp-div @local)
    [:div.entry-footer
     [:div
      [:button.pure-button.pure-button-primary
       {:on-click #(let [entry (:entry @local)]
                    (put-fn [:text-entry/persist entry])
                    (send-w-geolocation entry put-fn))}
       "save"]
      [:button.pure-button {:on-click #(put-fn [:import/photos])} [:span.fa.fa-camera-retro] " import"]]
     (let [tags (:tags (:entry @local))]
       (when (seq tags)
         [:div.hashtags
          (for [hashtag tags]
            ^{:key (str "tag-" hashtag)}
            [:span.hashtag hashtag])]))]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

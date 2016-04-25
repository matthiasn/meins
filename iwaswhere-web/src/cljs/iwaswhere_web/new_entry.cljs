(ns iwaswhere-web.new-entry
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.helpers :as h]
            [cljsjs.moment]
            [cljsjs.leaflet]
            [matthiasn.systems-toolbox.component :as st]))

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

(defn new-entry-view
  "Renders New Entry component."
  [{:keys [local put-fn]}]
  [:div.l-box-lrg.pure-g
   [:div.pure-u-1
    (let [tags (:tags (:entry @local))]
      [:div.hashtags
       (when (seq tags)
         (for [hashtag tags]
           ^{:key (str "tag-" hashtag)}
           [:span.hashtag.float-left hashtag]))])
    [:div.textentry
     [:textarea#new-entry-textbox
      {:type      "text"
       :value     (:md (:entry @local))
       ; TODO: occasionally store content into localstorage
       :on-change (fn [ev]
                    (let [prev-tags (or (:prev-tags @local) #{})
                          target (.. ev -target)
                          new-state (merge (h/parse-entry (.. target -value))
                                           {:timestamp (st/now)})
                          cursor-pos (.. target -selectionEnd)
                          new-tags (:tags new-state)]
                      (swap! local assoc-in [:entry] new-state)
                      (put-fn [:new-entry/tmp-save new-state])
                      (when (not= prev-tags new-tags)
                        (put-fn [:state/get {:tags new-tags}])
                        (swap! local assoc-in [:prev-tags] new-tags))))}]]
    #_(h/pp-div @local)
    [:div.entry-footer
      [:button.pure-button.pure-button-primary.button-xsmall
       {:on-click #(let [entry (merge (h/parse-entry "...")
                                      {:timestamp (st/now)})]
                    (put-fn [:text-entry/persist entry])
                    (send-w-geolocation entry put-fn))}
       [:span.fa.fa-plus-square] " new"]
      [:button.pure-button.pure-button-primary.button-xsmall
       {:on-click #(let [entry (:entry @local)]
                    (put-fn [:text-entry/persist entry])
                    (send-w-geolocation entry put-fn))}
       [:span.fa.fa-floppy-o] " save"]
      [:button.pure-button.button-xsmall
       {:on-click #(put-fn [:import/photos])}
       [:span.fa.fa-camera-retro] " import"]
      [:button.pure-button.button-xsmall
       {:on-click #(put-fn [:import/geo])}
       [:span.fa.fa-map-o] " import"]
      [:button.pure-button.button-xsmall
       {:on-click #(put-fn [:import/phone])}
       [:span.fa.fa-mobile-phone] " import"]]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

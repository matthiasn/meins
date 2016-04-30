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
  (let [on-change-fn #(let [prev-tags (or (:prev-tags @local) #{})
                            prev-mentions (or (:prev-mentions @local) #{})
                            new-state (merge (h/parse-entry (.. % -target -value)) {:timestamp (st/now)})
                            new-tags (:tags new-state)
                            new-mentions (:mentions new-state)]
                       (swap! local assoc-in [:entry] new-state)
                       (put-fn [:new-entry/tmp-save new-state])
                       (when (or (not= prev-tags new-tags) (not= prev-mentions new-mentions))
                         (put-fn [:state/get new-state])
                         (swap! local assoc-in [:prev-tags] new-tags)
                         (swap! local assoc-in [:prev-mentions] new-mentions)))
        new-entry-fn #(let [ts (st/now)
                            entry (merge (h/parse-entry "...") {:timestamp ts})]
                       (put-fn [:geo-entry/persist entry])
                       (put-fn [:cmd/toggle {:timestamp ts :key :show-edit-for}])
                       (send-w-geolocation entry put-fn))
        save-entry-fn #(let [entry (:entry @local)]
                        (put-fn [:geo-entry/persist entry])
                        (send-w-geolocation entry put-fn))]
    [:div.l-box-lrg.pure-g
     [:div.pure-u-1
      (let [tags (:tags (:entry @local))]
        [:div.hashtags
         (when (seq tags)
           (for [hashtag tags]
             ^{:key (str "tag-" hashtag)}
             [:span.hashtag.float-left hashtag]))])
      [:div.textentry
       [:textarea#new-entry-textbox {:type      "text"
                                     :value     (:md (:entry @local))
                                     :on-change on-change-fn}]]
      [:div.entry-footer
       [:button.pure-button.pure-button-primary.button-xsmall {:on-click new-entry-fn}
        [:span.fa.fa-plus-square] " new"]
       [:button.pure-button.pure-button-primary.button-xsmall {:on-click save-entry-fn}
        [:span.fa.fa-floppy-o] " save"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/photos])}
        [:span.fa.fa-camera-retro] " import"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/geo])}
        [:span.fa.fa-map-o] " import"]
       [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/phone])}
        [:span.fa.fa-mobile-phone] " import"]]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

(ns iwaswhere-web.ui.new-entry
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [iwaswhere-web.helpers :as h]))

(defn new-entry-view
  "Renders New Entry component."
  [{:keys [local put-fn]}]
  [:div.l-box-lrg.pure-g
   [:div.pure-u-1
    [:div.entry-footer
     [:button.pure-button.pure-button-primary.button-xsmall {:on-click (h/new-entry-fn put-fn {})}
      [:span.fa.fa-plus-square] " new"]
     [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/photos])}
      [:span.fa.fa-camera-retro] " import"]
     [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/geo])}
      [:span.fa.fa-map-o] " import"]
     [:button.pure-button.button-xsmall {:on-click #(put-fn [:import/phone])}
      [:span.fa.fa-mobile-phone] " import"]]]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-entry-view
              :dom-id  "new-entry"}))

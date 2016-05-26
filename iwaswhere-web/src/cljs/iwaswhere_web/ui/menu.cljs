(ns iwaswhere-web.ui.menu
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn cfg-view
  "Renders component for toggling display of maps, comments, ..."
  [store-snapshot put-fn]
  (let [show-all-maps? (:show-all-maps store-snapshot)
        toggle-all-maps #(put-fn [:cmd/toggle-key {:key :show-all-maps}])
        show-tags? (:show-hashtags store-snapshot)
        toggle-tags #(put-fn [:cmd/toggle-key {:key :show-hashtags}])
        show-context? (:show-context store-snapshot)
        toggle-context #(put-fn [:cmd/toggle-key {:key :show-context}])
        show-pvt? (:show-pvt store-snapshot)
        toggle-pvt #(put-fn [:cmd/toggle-key {:key :show-pvt}])
        sort-by-upvotes? (:sort-by-upvotes store-snapshot)
        toggle-upvotes #(do (put-fn [:cmd/toggle-key {:key :sort-by-upvotes}])
                            (put-fn [:state/get (merge (:current-query store-snapshot)
                                                       {:sort-by-upvotes (not sort-by-upvotes?)})]))]
    [:div
     [:span.fa.fa-thumbs-up.toggle {:class (when-not sort-by-upvotes? "inactive") :on-click toggle-upvotes}]
     [:span.fa.fa-user-secret.toggle {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}]
     [:span.fa.fa-eye.toggle {:class (when-not show-context? "inactive") :on-click toggle-context}]
     [:span.fa.fa-hashtag.toggle {:class (when-not show-tags? "inactive") :on-click toggle-tags}]
     [:span.fa.toggle {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}]]))

(defn new-import-view
  "Renders component for rendering new and import buttons."
  [{:keys [observed put-fn]}]
  [:div.menu-header
   [:div
    [:button.button-primary {:on-click (h/new-entry-fn put-fn {})}
     [:span.fa.fa-plus-square] " new"]
    [:button {:on-click #(do (put-fn [:import/photos]) (put-fn [:import/geo]) (put-fn [:import/phone]))}
     [:span.fa.fa-map] " import"]]
   [:h1 "iWasWhere?"]
   [cfg-view @observed put-fn]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-import-view
              :dom-id  "header"}))

(ns iwaswhere-web.ui.new-import
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
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
     [:span.fa.fa-thumbs-up.toggle.tooltip
      {:class (when-not sort-by-upvotes? "inactive") :on-click toggle-upvotes}
      [:span.tooltiptext "sort by upvotes first"]]
     [:span.fa.fa-user-secret.toggle.tooltip
      {:class (when-not show-pvt? "inactive") :on-click toggle-pvt}
      [:span.tooltiptext "show private entries"]]
     [:span.fa.fa-eye.toggle.tooltip
      {:class (when-not show-context? "inactive") :on-click toggle-context}
      [:span.tooltiptext "show query results"]]
     [:span.fa.fa-hashtag.toggle.tooltip
      {:class (when-not show-tags? "inactive") :on-click toggle-tags}
      [:span.tooltiptext "show hashtag symbol"]]
     [:span.fa.toggle.tooltip
      {:class (if show-all-maps? "fa-map" "fa-map-o") :on-click toggle-all-maps}
      [:span.tooltiptext "show all maps"]]]))

(defn new-import-view
  "Renders component for rendering new and import buttons."
  [{:keys [observed put-fn]}]
  [:div.menu-header
   [:div
    [u/btn-w-tooltip "fa-plus-square" "new" "new entry" (h/new-entry-fn put-fn {})]
    [u/btn-w-tooltip "fa-map" "import" "import" #(do (put-fn [:import/photos])
                                                     (put-fn [:import/geo])
                                                     (put-fn [:import/phone]))]]
   [:h1 "iWasWhere?"]
   [cfg-view @observed put-fn]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-import-view
              :dom-id  "header"}))

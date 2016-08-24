(ns iwaswhere-web.ui.menu
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn toggle-option-view
  "Render button for toggle option."
  [{:keys [option cls]} cfg put-fn]
  (let [show-option? (option cfg)
        toggle-option #(put-fn [:cmd/toggle-key {:path [:cfg option]}])]
    [:span.fa.toggle
     {:class    (str cls (when-not show-option? " inactive"))
      :on-click toggle-option}]))

(defn cfg-view
  "Renders component for toggling display of maps, comments, ..."
  [store-snapshot put-fn]
  (let [cfg (:cfg store-snapshot)
        sort-by-upvotes? (:sort-by-upvotes cfg)
        toggle-upvotes
        #(let [query (merge (:current-query store-snapshot)
                            {:sort-by-upvotes (not sort-by-upvotes?)})]
          (put-fn [:cmd/toggle-key {:path [:cfg :sort-by-upvotes]}])
          (put-fn [:state/get query]))]
    [:div
     [:span.fa.fa-thumbs-up.toggle
      {:class (when-not sort-by-upvotes? "inactive") :on-click toggle-upvotes}]
     (for [option (:toggle-options cfg)]
       ^{:key (str "toggle" (:cls option))}
       [toggle-option-view option cfg put-fn])
     [:span.fa.fa-ellipsis-h.toggle
      {:on-click #(put-fn [:cmd/toggle-lines])}]]))

(defn new-import-view
  "Renders component for rendering new and import buttons."
  [{:keys [observed put-fn]}]
  [:div.menu-header
   [:div
    [:button.menu-new {:on-click (h/new-entry-fn put-fn {})}
     [:span.fa.fa-plus-square] " new"]
    [:button {:on-click #(do (put-fn [:import/photos])
                             (put-fn [:import/geo])
                             (put-fn [:import/weight])
                             (put-fn [:import/phone]))}
     [:span.fa.fa-map] " import"]]
   [:h1 "iWasWhere?"]
   [cfg-view @observed put-fn]
   [:img {:src "/upload-address.png"}]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-import-view
              :dom-id  "header"}))

(ns iwaswhere-web.ui.new-import
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn new-import-view
  "Renders component for rendering new and import buttons."
  [{:keys [put-fn]}]
  [:span.new-import
   [u/btn-w-tooltip "fa-plus-square" "new" "new entry" (h/new-entry-fn put-fn {}) "pure-button-primary"]
   [u/btn-w-tooltip "fa-map" "import" "import" #(do (put-fn [:import/photos])
                                                    (put-fn [:import/geo])
                                                    (put-fn [:import/phone]))]])

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn new-import-view
              :dom-id  "new-import"}))

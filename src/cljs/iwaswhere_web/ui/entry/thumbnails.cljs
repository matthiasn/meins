(ns iwaswhere-web.ui.entry.thumbnails
  (:require [iwaswhere-web.ui.media :as m]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [iwaswhere-web.utils.misc :as u]
            [clojure.string :as s]
            [cljs.pprint :as pp]))

(defn image-view
  "Renders image view. Used resized and properly rotated image endpoint
   when JPEG file requested."
  [entry query-params]
  (when-let [file (:img-file entry)]
    (let [path (str "/photos/" file)
          resized (if (s/includes? path ".JPG")
                    (str "/photos2/" file query-params)
                    path)]
      [:div
       [:img {:src resized}]
       #_[:p.legend (:md entry)]])))

(defn carousel
  "Renders react-responsive-carousel with linked images."
  [entry linked]
  (let [react-responsive-carousel (aget js/window "deps" "react-responsive-carousel")
        ts (:timestamp entry)]
    (when (seq linked)
      (into
        [:> react-responsive-carousel]
        (mapv (fn [img-entry] (image-view img-entry "?width=600")) linked)))))

(defn thumbnails
  "Renders thumbnails of photos in linked entries. Respects private entries."
  [entry put-fn]
  (let [entries-map (subscribe [:entries-map])
        cfg (subscribe [:cfg])
        options (subscribe [:options])
        active (reaction (:active @cfg))
        show-pvt? (reaction (:show-pvt @cfg))]
    (fn thumbnail-render [entry put-fn]
      (let [ts (:timestamp entry)
            entry-active? (contains? (set (vals @active)) (:timestamp entry))
            linked-entries-set (set (:linked-entries-list entry))
            get-or-retrieve (u/find-missing-entry @entries-map put-fn)
            with-imgs (filter :img-file (map get-or-retrieve linked-entries-set))
            filtered (if @show-pvt?
                       with-imgs
                       (filter (u/pvt-filter @options @entries-map) with-imgs))]
        (when-not entry-active?
          [:div.thumbnails
           [carousel entry filtered]])))))

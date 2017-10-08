(ns iww.electron.renderer.ui.entry.thumbnails
  (:require [iww.electron.renderer.ui.media :as m]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [iwaswhere-web.utils.misc :as u]
            [clojure.string :as s]
            [cljs.pprint :as pp]
            [iwaswhere-web.utils.parse :as up]))

(defn image-view
  "Renders image view. Used resized and properly rotated image endpoint
   when JPEG file requested."
  [entry query-params local-cfg put-fn]
  (when-let [file (:img-file entry)]
    (let [path (str "/photos/" file)
          resized (if (s/includes? (s/lower-case path) ".jpg")
                    (str "/photos2/" file query-params)
                    path)
          tab-group (:tab-group local-cfg)
          add-search (up/add-search (str (:timestamp entry)) tab-group put-fn)]
      [:div {:on-click add-search}
       [:img {:src resized}]
       [:p.legend
        [:a {:href path :target "_blank"}
         [:span.fa.fa-expand]]]])))

(defn carousel
  "Renders react-responsive-carousel with linked images."
  [ts linked local-cfg put-fn]
  (let [responsive-carousel (aget js/window "deps" "react-responsive-carousel")]
    (fn [ts linked local-cfg put-fn]
      (when (seq linked)
        (into
          [:> responsive-carousel]
          (mapv (fn [img-entry] (image-view img-entry "?width=600" local-cfg put-fn)) linked))))))

(defn thumbnails
  "Renders thumbnails of photos in linked entries. Respects private entries."
  [entry local-cfg put-fn]
  (let [entries-map (subscribe [:entries-map])
        cfg (subscribe [:cfg])
        options (subscribe [:options])
        active (reaction (:active @cfg))
        show-pvt? (reaction (:show-pvt @cfg))
        get-or-retrieve (u/find-missing-entry entries-map put-fn)]
    (fn thumbnail-render [entry local-cfg put-fn]
      (let [ts (:timestamp entry)
            entry-active? (contains? (set (vals @active)) (:timestamp entry))
            linked-entries-set (set (:linked-entries-list entry))
            with-imgs (filter :img-file (map get-or-retrieve linked-entries-set))
            filtered (if @show-pvt?
                       with-imgs
                       (filter (u/pvt-filter @options @entries-map) with-imgs))]
        (when-not entry-active?
          [:div.thumbnails
           [carousel ts filtered local-cfg put-fn]])))))

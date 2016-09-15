(ns iwaswhere-web.ui.entry.thumbnails
  (:require [iwaswhere-web.ui.media :as m]
            [iwaswhere-web.utils.misc :as u]))

(defn thumbnails
  "Renders thumbnails of photos in linked entries. Respects private entries."
  [entry entries-map cfg put-fn]
  (let [ts (:timestamp entry)
        entry-active? (contains? (set (vals (:active cfg))) (:timestamp entry))
        linked-entries-set (set (:linked-entries-list entry))
        get-or-retrieve (u/find-missing-entry entries-map put-fn)
        with-imgs (filter :img-file (map get-or-retrieve linked-entries-set))
        filtered (if (:show-pvt cfg)
                   with-imgs
                   (filter (u/pvt-filter cfg) with-imgs))]
    (when-not entry-active?
      [:div.thumbnails
       (for [img-entry filtered]
         ^{:key (str "thumbnail" ts (:img-file img-entry))}
         [:div [m/image-view img-entry "?width=300"]])])))

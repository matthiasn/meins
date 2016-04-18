(ns iwaswhere-web.image)

(defn image-view
  "Renders image view."
  [entry]
  (when-let [img-file (:img-file entry)]
    [:a {:href (str "/photos/" img-file) :target "_blank"}
     [:img {:src (str "/photos/" img-file)}]]))

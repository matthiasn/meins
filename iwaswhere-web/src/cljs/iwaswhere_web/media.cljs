(ns iwaswhere-web.media)

(defn image-view
  "Renders image view."
  [entry]
  (when-let [img-file (:img-file entry)]
    [:a {:href (str "/photos/" img-file) :target "_blank"}
     [:img {:src (str "/photos/" img-file)}]]))

(defn audioplayer-view
  "Renders audio player view."
  [entry]
  (when-let [audio-file (:audio-file entry)]
    [:audio {:controls true :preload "none"}
     [:source {:src (str "/audio/" audio-file) :type "audio/mp4"}]]))

(defn videoplayer-view
  "Renders video player view."
  [entry]
  (when-let [video-file (:video-file entry)]
    [:video {:controls true :preload "none"}
     [:source {:src (str "/videos/" video-file) :type "video/mp4"}]]))

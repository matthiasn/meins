(ns iww.electron.renderer.ui.media
  (:require [clojure.string :as s]
            [reagent.core :as r]
            [clojure.pprint :as pp]))

(def iww-host (.-iwwHOST js/window))

(defn image-view
  "Renders image view. Used resized and properly rotated image endpoint
   when JPEG file requested."
  [entry query-params]
  (when-let [file (:img-file entry)]
    (let [path (str "http://" iww-host "/photos/" file)
          resized (if (s/includes? (s/lower-case path) ".jpg")
                    (str "http://" iww-host "/photos2/" file query-params)
                    path)]
      [:a {:href path :target "_blank"}
       [:img {:src resized}]])))

(defn audioplayer-view
  "Renders audio player view."
  [entry put-fn]
  (when-let [audio-file (:audio-file entry)]
    [:audio {:id       audio-file
             :controls true
             :preload  "auto"}
     (let [elem (js->clj (.getElementById js/document audio-file))
           duration (when elem (.. elem -duration))
           path [:custom-fields "#audio" :duration]]
       (when (and duration (not (js/isNaN duration)))
         (when-not (get-in entry path)
           (let [updated (assoc-in entry path (js/parseInt duration))]
             (put-fn [:entry/update-local updated])))))
     [:source {:src  (str "http://" iww-host "/audio/" audio-file)
               :type "audio/mp4"}]]))

(defn videoplayer-view
  "Renders video player view."
  [entry]
  (when-let [video-file (:video-file entry)]
    [:video {:controls true :preload "none"}
     [:source {:src  (str "http://" iww-host "/videos/" video-file)
               :type "video/mp4"}]]))

(defn imdb-view
  "Renders IMDb view."
  [entry put-fn]
  (when-let [imdb-id (get-in entry [:custom-fields "#imdb" :imdb-id])]
    (let [imdb (:imdb entry)
          series (:series imdb)]
      (if imdb
        [:div
         (if series
           [:h4 (:title series) " S" (:season imdb) "E" (:episode imdb)
            ": " (:title imdb) " - " (:year imdb)]
           [:h4 (:title imdb) " - " (:year imdb)])
         [:p (:actors imdb)]
         [:p (:plot imdb)]
         (when-let [series-poster (:poster series)]
           [:img {:src series-poster}])
         [:img {:src (:poster imdb)}]]
        (put-fn [:import/movie {:entry   entry
                                :imdb-id imdb-id}])))))

(defn spotify-view
  "Renders IMDb view."
  [entry put-fn]
  (when-let [spotify (get-in entry [:spotify])]
    [:div.spotify
     [:div.title (:name spotify)]
     [:div.artist (:name (first (:artists spotify)))]
     [:img {:src (:image spotify)}]]))

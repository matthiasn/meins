(ns iwaswhere-web.index
  "This namespace takes care of rendering the static HTML into which the
   React / Reagent components are mounted on the client side at runtime."
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [compojure.route :as r]
            [iwaswhere-web.routes.upload-qr :as qr]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.routes.images :as ir]
            [iwaswhere-web.routes.map-tile :as mt]
            [iwaswhere-web.file-utils :as fu]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html5
    {:lang "en"}
    [:head
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title "iWasWhere"]
     ; Download from https://github.com/christiannaths/Redacted-Font
     ; then uncomment in _entry.scss and recompile CSS for redacted fron
     (include-css "/redacted-font/fonts/web/stylesheet.css")
     (include-css "/webjars/normalize-css/5.0.0/normalize.css")
     (include-css "/webjars/github-com-mrkelly-lato/0.3.0/css/lato.css")
     (include-css "https://fonts.googleapis.com/css?family=Oswald:300,400")
     (include-css "/webjars/fontawesome/4.7.0/css/font-awesome.css")
     (include-css "/webjars/leaflet/0.7.7/dist/leaflet.css")
     (include-css "/css/carousel.css")
     (include-css "/css/iwaswhere.css")]
    [:body
     [:div#reframe]
     ;; Currently, from http://www.orangefreesounds.com/old-clock-ringing-short/
     ;; TODO: record own alarm clock
     [:audio#ringer {:autoPlay false :loop false}
      [:source {:src "/mp3/old-clock-ringing-short.mp3" :type "audio/mp4"}]]
     [:audio#ticking-clock {:autoPlay false :loop false}
      [:source {:src "/mp3/tick.ogg" :type "audio/ogg"}]]
     (include-js "/webjars/leaflet/0.7.7/dist/leaflet.js")
     (include-js "/webjars/intl/1.2.4/dist/Intl.min.js")
     (include-js "/webjars/intl/1.2.4/locale-data/jsonp/en.js")
     (include-js "/webjars/randomcolor/0.4.4/randomColor.js")
     (include-js "/js/bundle.js")
     (include-js "/js/build/iwaswhere.js")]))

(defn routes-fn
  "Adds routes for serving media files. This routes function will receive the
   put-fn of the ws-cmp, which is not used here but can be useful in scenarios
   when requests are supposed to be handled by a another component."
  [_put-fn]
  [(r/files "/photos" {:root (str fu/data-path "/images/")})
   (r/files "/audio" {:root (str fu/data-path "/audio/")})
   (r/files "/videos" {:root (str fu/data-path "/videos/")})
   qr/address-qr-route
   ir/img-resized-route
   mt/map-tile-route])

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :routes-fn     routes-fn
   :port          8765
   :relay-types   #{:cmd/keep-alive-res :entry/saved :entry/found :state/new
                    :stats/result :stats/result2 :state/stats-tags :search/refresh}})

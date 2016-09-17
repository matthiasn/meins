(ns iwaswhere-web.index
  "This namespace takes care of rendering the static HTML into which the
   React / Reagent components are mounted on the client side at runtime."
  (:require [hiccup.core :refer [html]]
            [compojure.route :as r]
            [iwaswhere-web.upload-qr :as qr]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.img-route :as ir]))

(defn stylesheet [url] [:link {:href url :rel "stylesheet"}])

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "iWasWhere"]
      ; Download from https://github.com/christiannaths/Redacted-Font
      ; then uncomment in _entry.scss and recompile CSS for redacted fron
      (stylesheet "/redacted-font/fonts/web/stylesheet.css")
      (stylesheet "/webjars/normalize-css/4.1.1/normalize.css")
      (stylesheet "/webjars/github-com-mrkelly-lato/0.3.0/css/lato.css")
      (stylesheet "/webjars/fontawesome/4.6.3/css/font-awesome.css")
      (stylesheet "/webjars/leaflet/0.7.7/dist/leaflet.css")
      (stylesheet "/css/iwaswhere.css")]
     [:body
      [:div.flex-container
       [:div#header]
       [:div#content]
       [:div#stats]]
      ;; Currently, from http://www.orangefreesounds.com/old-clock-ringing-short/
      ;; TODO: record own alarm clock
      [:audio#ringer {:autoPlay false :loop false}
       [:source {:src "/mp3/old-clock-ringing-short.mp3" :type "audio/mp4"}]]
      [:audio#ticking-clock {:autoPlay false :loop false}
       [:source {:src "/mp3/tick.ogg" :type "audio/ogg"}]]
      [:script {:src "/webjars/intl/1.2.4/dist/Intl.min.js"}]
      [:script {:src "/webjars/intl/1.2.4/locale-data/jsonp/en.js"}]
      [:script {:src "/js/build/iwaswhere.js"}]]]))

(defn routes-fn
  "Adds routes for serving media files. This routes function will receive the
   put-fn of the ws-cmp, which is not used here but can be useful in scenarios
   when requests are supposed to be handled by a another component."
  [_put-fn]
  [(r/files "/photos" {:root (str f/data-path "/images/")})
   (r/files "/audio" {:root (str f/data-path "/audio/")})
   (r/files "/videos" {:root (str f/data-path "/videos/")})
   qr/address-qr-route
   ir/img-resized-route])

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :routes-fn     routes-fn
   :port          8765
   :relay-types   #{:cmd/keep-alive-res :entry/saved :entry/found :state/new
                    :stats/result :state/stats-tags :search/refresh}})

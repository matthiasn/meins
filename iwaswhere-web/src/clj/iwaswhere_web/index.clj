(ns iwaswhere-web.index
  "This namespace takes care of rendering the static HTML into which the React / Reagent
  components are mounted on the client side at runtime."
  (:require [hiccup.core :refer [html]]
            [compojure.route :as r]
            [iwaswhere-web.files :as f]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "iWasWhere"]
      [:link {:href "/webjars/normalize-css/3.0.3/normalize.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/webjars/github-com-mrkelly-lato/0.3.0/css/lato.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/webjars/fontawesome/4.6.3/css/font-awesome.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/webjars/leaflet/0.7.7/dist/leaflet.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/css/iwaswhere.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/images/favicon.png" :rel "shortcut icon" :type "image/png"}]]
     [:body
      [:div.flex-container
       [:div#header]
       [:div#search]
       [:div#journal]]
      ;; Currently, sounds from http://www.orangefreesounds.com/old-clock-ringing-short/
      ;; TODO: record own alarm clock
      [:audio#ringer {:autoPlay false :loop false}
       [:source {:src "/mp3/old-clock-ringing-short.mp3" :type "audio/mp4"}]]
      [:audio#ticking-clock {:autoPlay false :loop false}
       [:source {:src "/mp3/tick.ogg" :type "audio/ogg"}]]
      [:script {:src "/js/build/iwaswhere.js"}]]]))

(defn routes-fn
  "Adds routes for serving media files. This routes function will receive the put-fn of the ws-cmp,
   which is not used here but can be useful in scenarios when requests are supposed to be handled
   by a another component."
  [_put-fn]
  [(r/files "/photos" {:root (str f/data-path "/images/")})
   (r/files "/audio" {:root (str f/data-path "/audio/")})
   (r/files "/videos" {:root (str f/data-path "/videos/")})])

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :routes-fn     routes-fn
   :relay-types   #{:cmd/keep-alive-res :entry/saved :state/new}})

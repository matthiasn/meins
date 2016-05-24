(ns iwaswhere-web.index
  "This namespace takes care of rendering the static HTML into which the React / Reagent
  components are mounted on the client side at runtime."
  (:require [hiccup.core :refer [html]]
            [compojure.route :as r]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "iWasWhere"]
      [:link {:href "/bower_components/normalize-css/normalize.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/lato/css/lato.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/font-awesome/css/font-awesome.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/leaflet/dist/leaflet.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/css/iwaswhere.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/images/favicon.png" :rel "shortcut icon" :type "image/png"}]]
     [:body
      [:div.flex-container
       [:div#header]
       [:div#search]
       [:div#journal]]
      [:script {:src "/js/build/iwaswhere.js"}]]]))

(defn routes-fn
  "Adds a route for serving photos. This routes function will receive the put-fn of the ws-cmp, which
  is not used here but can be useful in scenarios when requests are supposed to be handled by a another
  component."
  [_put-fn]
  [(r/files "/photos" {:root "data/images/"})
   (r/files "/audio" {:root "data/audio/"})
   (r/files "/videos" {:root "data/videos/"})])

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :routes-fn     routes-fn})

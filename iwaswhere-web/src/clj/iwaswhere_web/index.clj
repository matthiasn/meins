(ns iwaswhere-web.index
  (:require [hiccup.core :refer [html]]
            [compojure.route :as r]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "iWasWhere"]
      [:link {:href "/bower_components/lato/css/lato.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/pure-min.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/font-awesome/css/font-awesome.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/grids-responsive-min.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/bower_components/leaflet/dist/leaflet.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/css/iwaswhere.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/images/favicon.png" :rel "shortcut icon" :type "image/png"}]]
     [:body
      [:div.header
       [:div.home-menu.pure-menu.pure-menu-open.pure-menu-horizontal.pure-menu-fixed
        [:a.pure-menu-heading {:href ""} "iWasWhere?"]]]
      [:div.content-wrapper
       [:div.content [:div#search]]
       [:div.content [:div#new-entry]]
       [:div.content [:div#journal]]]
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
  {:index-page-fn index-page
   :routes-fn     routes-fn})

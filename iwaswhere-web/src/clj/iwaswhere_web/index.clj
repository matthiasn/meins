(ns iwaswhere-web.index
  (:require [hiccup.core :refer [html]]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "iWasWhere"]
      [:link {:href "/bower_components/lato/css/lato.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/grids-responsive-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/bower_components/leaflet/dist/leaflet.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/css/example.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/images/favicon.png", :rel "shortcut icon", :type "image/png"}]]
     [:body
      [:div.header
       [:div.home-menu.pure-menu.pure-menu-open.pure-menu-horizontal.pure-menu-fixed
        [:a.pure-menu-heading {:href ""} "iWasWhere?"]]]
      [:div.content-wrapper
       [:div.content [:div#journal]]
       [:div.content [:div#map]]]
      [:script {:src "/js/build/iwaswhere.js"}]]]))

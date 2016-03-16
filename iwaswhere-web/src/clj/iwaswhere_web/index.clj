(ns iwaswhere-web.index
  (:require [hiccup.core :refer [html]]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [dev?]
  (html
    [:html
     {:lang "en"}
     [:head
      ;[:meta {:content "width=device-width, user-scalable=no", :name "viewport"}]
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "iWasWhere"]
      [:link {:href "/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/grids-responsive-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/css/example.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/images/favicon.png", :rel "shortcut icon", :type "image/png"}]]
     [:body
      [:div.header
       [:div.home-menu.pure-menu.pure-menu-open.pure-menu-horizontal.pure-menu-fixed
        [:a.pure-menu-heading {:href ""} "iWasWhere?"]
        [:ul
         [:li [:div#jvm-stats-frame]]]]]
      [:div.content-wrapper
       [:div.content
        [:h2.content-head.is-center "iWasWhere? - tracking places, thoughts and tasks"]]
       [:div.content [:div#journal]]]
      [:script {:src "/js/build/iwaswhere.js"}]]]))

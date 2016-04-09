(defproject matthiasn/iwaswhere-web "0.1.1-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url  "http://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [ch.qos.logback/logback-classic "1.1.6"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.11.0"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "0.9.86"]
                 [cheshire "5.5.0"]
                 [cljsjs/moment "2.10.6-3"]
                 [cljsjs/leaflet "0.7.7-2"]
                 [com.drewnoakes/metadata-extractor "2.8.1"]
                 [ubergraph "0.2.1"]
                 [camel-snake-kebab "0.3.2"]
                 [matthiasn/systems-toolbox "0.5.16"]
                 [matthiasn/systems-toolbox-ui "0.5.7"]
                 [matthiasn/systems-toolbox-sente "0.5.14"]
                 [clj-time "0.11.0"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/"]

  :main iwaswhere-web.core

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.2"]
            [lein-codox "0.9.4"]]

  :figwheel {:server-port 3450
             :css-dirs    ["resources/public/css"]}

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/cljs" "env/dev/cljs"]
                        :figwheel     true
                        :compiler     {:main          "iwaswhere-web.dev"
                                       :asset-path    "js/build"
                                       :optimizations :none
                                       :output-dir    "resources/public/js/build/"
                                       :output-to     "resources/public/js/build/iwaswhere.js"
                                       :source-map    true}}
                       {:id           "release"
                        :source-paths ["src/cljs"]
                        :figwheel     true
                        :compiler     {:main          "iwaswhere-web.core"
                                       :asset-path    "js/build"
                                       :output-to     "resources/public/js/build/iwaswhere.js"
                                       :optimizations :simple}}]})

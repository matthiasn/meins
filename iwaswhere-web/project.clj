(defproject matthiasn/iwaswhere-web "0.1.1-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url  "http://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.11.0"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "0.9.89"]
                 [cheshire "5.6.1"]
                 [cljsjs/moment "2.10.6-4"]
                 [cljsjs/leaflet "0.7.7-4"]
                 [com.drewnoakes/metadata-extractor "2.8.1"]
                 [ubergraph "0.2.1"]
                 [camel-snake-kebab "0.3.2"]
                 [matthiasn/systems-toolbox "0.5.19"]
                 [matthiasn/systems-toolbox-ui "0.5.8"]
                 [matthiasn/systems-toolbox-sente "0.5.16"]
                 [alandipert/storage-atom "2.0.1"]
                 [clj-time "0.11.0"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/"]
  :auto-clean false

  :main iwaswhere-web.core
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.3-2"]
            [lein-sassy "1.0.7"]
            [com.jakemccrary/lein-test-refresh "0.15.0"]
            [lein-codox "0.9.5"]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

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
                                       :externs       ["externs/misc.js" "externs/leaflet.ext.js"]
                                       :output-to     "resources/public/js/build/iwaswhere.js"
                                       :optimizations :advanced}}]})

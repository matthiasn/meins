(defproject matthiasn/iwaswhere-web "0.1.44"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE"
            :url  "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/clojurescript "1.9.521"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.13.0"]
                 [clj-http "3.5.0"]
                 [enlive "1.1.6"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "0.9.99"]
                 [cheshire "5.7.1"]
                 [cljsjs/moment "2.17.1-0"]
                 [com.drewnoakes/metadata-extractor "2.10.1"]
                 [ubergraph "0.3.1"]
                 [factual/geo "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [matthiasn/systems-toolbox "0.6.8"]
                 [matthiasn/systems-toolbox-sente "0.6.14"]
                 [reagent "0.6.1"]
                 [capacitor "0.6.0"]
                 [re-frame "0.9.2"]

                 [io.zipkin.brave/brave "4.3.0"]
                 [io.zipkin.reporter/zipkin-reporter "0.9.0"]
                 [io.zipkin.reporter/zipkin-sender-okhttp3 "0.9.0"]

                 [clucy "0.4.0"]
                 [seesaw "1.4.5"]
                 [clj.qrgen "0.4.0"]
                 [image-resizer "0.1.9"]
                 [danlentz/clj-uuid "0.1.7"]
                 [org.webjars.bower/fontawesome "4.7.0"]
                 [org.webjars.npm/randomcolor "0.4.4"]
                 [org.webjars.bower/normalize-css "5.0.0"]
                 [org.webjars.bower/leaflet "0.7.7"]
                 [org.webjars.npm/github-com-mrkelly-lato "0.3.0"]
                 [org.webjars.npm/intl "1.2.4"]
                 [alandipert/storage-atom "2.0.1"]]

  :source-paths ["src/cljc" "src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/" "packages/"]
  :auto-clean false

  :main iwaswhere-web.core
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[re-frisk "0.4.5"]]}}

  :plugins [[lein-cljsbuild "1.1.6"
             :exclusions [org.apache.commons/commons-compress]]
            [lein-figwheel "0.5.10"]
            [lein-sassy "1.0.8"
             :exclusions [org.clojure/clojure org.codehaus.plexus/plexus-utils]]
            [com.jakemccrary/lein-test-refresh "0.20.0"]
            [test2junit "1.3.0"]
            [lein-doo "0.1.7"]
            [lein-codox "0.10.3"]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

  ;:global-vars {*assert* false}

  :figwheel {:server-port 3450
             :css-dirs    ["resources/public/css"]}

  :test-refresh {:notify-on-success false
                 :changes-only      false
                 :watch-dirs        ["src" "test"]}

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "release"]
                     ["sass" "once"] "uberjar"]}

  :codox {:output-path "codox"
          :source-uri  "https://github.com/matthiasn/iWasWhere/blob/master/iwaswhere-web/{filepath}#L{line}"}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljc" "src/cljs" "env/dev/cljs"]
     :figwheel     true
     :compiler     {:main          "iwaswhere-web.dev"
                    :asset-path    "js/build"
                    :optimizations :none
                    ;:output-dir    "resources/public/js/build/"
                    ;:output-to     "resources/public/js/build/iwaswhere.js"
                    :source-map    true}}
    {:id           "release"
     :source-paths ["src/cljc" "src/cljs"]
     :compiler     {:main          "iwaswhere-web.core"
                    :asset-path    "js/build"
                    :elide-asserts true
                    :externs       ["externs/misc.js"
                                    "externs/leaflet.ext.js"]
                    :output-dir    "resources/public/js/build/"
                    :output-to     "resources/public/js/build/iwaswhere.js"
                    ;:source-map    "resources/public/js/build/iwaswhere.js.map"
                    :optimizations :whitespace}}
    {:id           "cljs-test"
     :source-paths ["src/cljs" "src/cljc" "test"]
     :compiler     {:output-to     "out/testable.js"
                    :output-dir    "out/"
                    :main          iwaswhere-web.runner
                    :optimizations :whitespace}}]})

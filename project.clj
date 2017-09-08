(defproject matthiasn/iwaswhere-web "0.2.108"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE"
            :url  "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/clojurescript "1.9.908"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.14.0"]
                 [clj-http "3.7.0"]
                 [enlive "1.1.6"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "1.0.1"]
                 [clj-pdf "2.2.29"]
                 [cheshire "5.8.0"]
                 [com.taoensso/nippy "2.13.0"]
                 [cljsjs/moment "2.17.1-1"]
                 [com.drewnoakes/metadata-extractor "2.10.1"]
                 [ubergraph "0.4.0"]
                 [factual/geo "1.1.0"]
                 [camel-snake-kebab "0.4.0"]
                 [matthiasn/systems-toolbox-kafka "0.6.13"]
                 [matthiasn/systems-toolbox "0.6.14"]
                 [matthiasn/systems-toolbox-sente "0.6.16"]
                 [matthiasn/systems-toolbox-zipkin "0.6.3"]
                 [reagent "0.7.0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [re-frame "0.10.1"]
                 [secretary "1.2.3"]
                 [capacitor "0.6.0"]
                 [clucy "0.4.0"]
                 [seesaw "1.4.5"]
                 [clj.qrgen "0.4.0"]
                 [image-resizer "0.1.10"]
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
  :uberjar-name "iwaswhere.jar"

  :main iwaswhere-web.core
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow" "-XX:+AggressiveOpts"]

  :profiles {:uberjar  {:aot :all}
             :cljs-doo {:dependencies [[cljsjs/react "15.6.1-2"]
                                       [cljsjs/react-dom "15.6.1-2"]]}}

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :plugins [[lein-cljsbuild "1.1.7"
             :exclusions [org.apache.commons/commons-compress]]
            [lein-figwheel "0.5.13"]
            [lein-sassy "1.0.8"
             :exclusions [org.clojure/clojure org.codehaus.plexus/plexus-utils]]
            [com.jakemccrary/lein-test-refresh "0.21.1"]
            [test2junit "1.3.3"]
            [lein-doo "0.1.7"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.10"]
            [lein-codox "0.10.3"]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

  ;:global-vars {*assert* false}

  :figwheel {:server-port 3450
             :css-dirs    ["resources/public/css"]}

  :test-refresh {:notify-on-success false
                 :changes-only      false
                 :watch-dirs        ["src" "test"]}

  :test2junit-run-ant true

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "release"]
                     ["sass" "once"] "uberjar"]
            "dist"  ["do"
                     ["clean"]
                     ["test2junit"]
                     ["cljsbuild" "once" "release"]
                     ["sass" "once"]
                     ["shell" "npm" "install"]
                     ["shell" "webpack" "-p"]
                     ["uberjar"]
                     ["shell" "cp" "target/iwaswhere.jar" "electron-cljs/bin/"]
                     ["shell" "./publish.sh"]]
            "beta"  ["do"
                     ["clean"]
                     ["test2junit"]
                     ["cljsbuild" "once" "release"]
                     ["sass" "once"]
                     ["shell" "npm" "install"]
                     ["shell" "webpack" "-p"]
                     ["uberjar"]
                     ["shell" "cp" "target/iwaswhere.jar" "electron-cljs/bin/"]
                     ["shell" "./publish_beta.sh"]]}

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
                    :process-shim  false
                    :optimizations :whitespace}}]})

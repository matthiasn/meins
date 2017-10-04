(defproject matthiasn/iwaswhere-web "0.0-SNAPSHOT"
  :description "iWasWhere - a personal information manager"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE"
            :url  "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [org.clojure/clojurescript "1.9.946"]
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
                 [factual/geo "1.2.0"]
                 [camel-snake-kebab "0.4.0"]
                 [matthiasn/systems-toolbox-kafka "0.6.13"]
                 [matthiasn/systems-toolbox "0.6.19"]
                 [matthiasn/systems-toolbox-sente "0.6.17"]
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

  :profiles {:uberjar      {:aot :all}
             :test-reagent {:dependencies [[cljsjs/react "15.6.1-2"]
                                           [cljsjs/react-dom "15.6.1-2"]
                                           [cljsjs/create-react-class "15.6.0-2"]]}}

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :plugins [[lein-cljsbuild "1.1.7"
             :exclusions [org.apache.commons/commons-compress]]
            [lein-figwheel "0.5.14"]
            [test2junit "1.3.3"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.12"]]

  ;:global-vars {*assert* false}

  :test2junit-run-ant true

  :aliases {"sass"  ["shell" "sass" "src/scss/iwaswhere.scss" "resources/public/css/iwaswhere.css"]
            "build" ["do"
                     ["clean"]
                     ["test"]
                     ["cljsbuild" "once" "release"]
                     ["sass"]
                     ["shell" "npm" "install"]
                     ["shell" "webpack" "-p"]
                     ["uberjar"]
                     ["shell" "cp" "target/iwaswhere.jar" "electron-cljs/bin/"]]
            "dist"  ["do"
                     ["build"]
                     ["shell" "./publish.sh"]]
            "beta"  ["do"
                     ["build"]
                     ["shell" "./publish_beta.sh"]]}

  :cljsbuild {:test-commands {"cljs-test" ["phantomjs" "test/phantom/test.js" "test/phantom/test.html"]}
              :builds        [{:id           "release"
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

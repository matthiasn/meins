(defproject matthiasn/iwaswhere-electron "0.2.108"
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/clojurescript "1.9.908"]
                 [re-frame "0.10.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [matthiasn/systems-toolbox "0.6.14"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-sassy "1.0.8"]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

  :clean-targets ^{:protect false} ["resources/public/css/" "target/" "prod/"]

  :aliases {"dist" ["do"
                    ["clean"]
                    ["cljsbuild" "once" "main"]
                    ["cljsbuild" "once" "renderer"]
                    ["cljsbuild" "once" "updater"]
                    ["sass" "once"]]}

  :cljsbuild {:builds [{:id           "main"
                        :source-paths ["src/iwaswhere_electron/main"]
                        :compiler     {:main           iwaswhere-electron.main.core
                                       :target         :nodejs
                                       :output-to      "prod/main/main.js"
                                       :output-dir     "prod/main"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :parallel-build true}}
                       {:id           "renderer"
                        :source-paths ["src/iwaswhere_electron/renderer"]
                        :compiler     {:main           iwaswhere-electron.renderer.core
                                       :output-to      "prod/renderer/renderer.js"
                                       :target         :nodejs
                                       :output-dir     "prod/renderer"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log "2.2.7"
                                                        :moment       "2.18.1"
                                                        :react        "15.6.1"
                                                        :react-dom    "15.6.1"
                                                        :electron     "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :parallel-build true}}
                       {:id           "updater"
                        :source-paths ["src/iwaswhere_electron/update"]
                        :compiler     {:main           iwaswhere-electron.update.core
                                       :output-to      "prod/updater/update.js"
                                       :target         :nodejs
                                       :output-dir     "prod/updater"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log "2.2.7"
                                                        :moment       "2.18.1"
                                                        :react        "15.6.1"
                                                        :react-dom    "15.6.1"
                                                        :electron     "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :parallel-build true}}]})

(defproject matthiasn/iwaswhere-electron "0.2.43"
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/clojurescript "1.9.908"]
                 [re-frame "0.10.1"]
                 [matthiasn/systems-toolbox "0.6.11"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-sassy "1.0.8"]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

  :cljsbuild {:builds [{:id           "main"
                        :source-paths ["src/iwaswhere_electron/main"]
                        :compiler     {:main           iwaswhere-electron.main.core
                                       :target         :nodejs
                                       :output-to      "package/main.js"
                                       :output-dir     "package"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :react             "15.6.1"
                                                        :react-dom         "15.6.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :none
                                       :pretty-print   true
                                       :parallel-build true}}
                       {:id           "main-prod"
                        :source-paths ["src/iwaswhere_electron/main"]
                        :compiler     {:main           iwaswhere-electron.main.core
                                       :target         :nodejs
                                       :output-to      "prod/main.js"
                                       :output-dir     "prod"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :react             "15.6.1"
                                                        :react-dom         "15.6.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :pretty-print   true
                                       :parallel-build true}}
                       {:id           "renderer"
                        :source-paths ["src/iwaswhere_electron/renderer"]
                        :compiler     {:main           iwaswhere-electron.renderer.core
                                       :output-to      "renderer/renderer.js"
                                       :target         :nodejs
                                       :output-dir     "renderer"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :react             "15.6.1"
                                                        :react-dom         "15.6.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :pretty-print   true
                                       :parallel-build true}}
                       {:id           "renderer-prod"
                        :source-paths ["src/iwaswhere_electron/renderer"]
                        :compiler     {:main           iwaswhere-electron.renderer.core
                                       :output-to      "renderer-prod/renderer.js"
                                       :target         :nodejs
                                       :output-dir     "renderer-prod"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :react             "15.6.1"
                                                        :react-dom         "15.6.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :pretty-print   true
                                       :parallel-build true}}
                       {:id           "updater"
                        :source-paths ["src/iwaswhere_electron/update"]
                        :compiler     {:main           iwaswhere-electron.update.core
                                       :output-to      "updater/update.js"
                                       :target         :nodejs
                                       :output-dir     "updater"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :react             "15.6.1"
                                                        :react-dom         "15.6.1"
                                                        :electron-builder  "19.24.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron-packager "8.7.2"
                                                        :electron          "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :pretty-print   true
                                       :parallel-build true}}]})

(defproject matthiasn/iwaswhere-electron "0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [org.clojure/clojurescript "1.9.908"]
                 [re-frame "0.10.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [matthiasn/systems-toolbox "0.6.15"]
                 [matthiasn/systems-toolbox-electron "0.6.6"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-shell "0.5.0"]]

  :clean-targets ^{:protect false} ["target/" "prod/"]

  :aliases {"sass"  ["do"
                     ["shell" "sass" "src/scss/updater.scss" "resources/public/css/updater.css"]
                     ["shell" "sass" "src/scss/loader.scss" "resources/public/css/loader.css"]]
            "build" ["do"
                     ["clean"]
                     ["cljsbuild" "once" "main"]
                     ["cljsbuild" "once" "renderer"]
                     ["cljsbuild" "once" "updater"]
                     ["sass"]]}

  :cljsbuild {:builds [{:id           "main"
                        :source-paths ["src/iwaswhere_electron/main"]
                        :compiler     {:main           iwaswhere-electron.main.core
                                       :target         :nodejs
                                       :output-to      "prod/main/main.js"
                                       :output-dir     "prod/main"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log      "2.2.7"
                                                        :moment            "2.18.1"
                                                        :electron-updater  "2.8.7"
                                                        :electron          "1.8.0"}
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
                                                        :electron     "1.8.0"}
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
                                                        :electron     "1.8.0"}
                                       :install-deps   true
                                       :optimizations  :advanced
                                       :parallel-build true}}]})

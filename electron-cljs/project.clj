(defproject matthiasn/iwaswhere-electron "0.2.40"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.908"]
                 [matthiasn/systems-toolbox "0.6.10"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :cljsbuild {:builds [{:id           "main"
                        :source-paths ["src/iwaswhere_electron/main"]
                        :compiler     {:main           iwaswhere-electron.main.core
                                       :output-to      "package/main/main.js"
                                       :target         :nodejs
                                       :output-dir     "package/main"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log          "2.2.7"
                                                        :moment                "2.18.1"
                                                        :react                 "15.6.1"
                                                        :react-dom             "15.6.1"
                                                        :electron-spellchecker "1.2.0"
                                                        :electron              "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :none
                                       :pretty-print   true
                                       :parallel-build true}}
                       {:id           "renderer"
                        :source-paths ["src/iwaswhere_electron/renderer"]
                        :compiler     {:main           iwaswhere-electron.renderer.core
                                       :output-to      "package/renderer/renderer.js"
                                       :target         :nodejs
                                       :output-dir     "package/renderer"
                                       :externs        ["externs.js"]
                                       :npm-deps       {:electron-log          "2.2.7"
                                                        :moment                "2.18.1"
                                                        :react                 "15.6.1"
                                                        :react-dom             "15.6.1"
                                                        :electron-spellchecker "1.2.0"
                                                        :electron              "1.7.6"}
                                       :install-deps   true
                                       :optimizations  :none
                                       :pretty-print   true
                                       :parallel-build true}}]})

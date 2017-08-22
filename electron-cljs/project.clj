(defproject matthiasn/iwaswhere-electron "0.2.40"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.908"]
                 [matthiasn/systems-toolbox "0.6.10"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :cljsbuild {:builds [{:id "release"
                        :source-paths ["src"]
                        :compiler {:main iwaswhere-electron.core
                                   :output-to "package/main.js"
                                   :target :nodejs
                                   :output-dir "package"
                                   :externs ["externs.js"]
                                   :npm-deps             {:electron-log "2.2.7"
                                                          :moment      "2.18.1"
                                                          :react "15.6.1"
                                                          :react-dom "15.6.1"
                                                          :electron "1.7.6"}
                                   :install-deps         true
                                   :optimizations :none
                                   :pretty-print true
                                   :parallel-build true}}]})

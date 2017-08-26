(ns iwaswhere-electron.main.startup
  (:require [iwaswhere-electron.main.log :as log]
            [child_process :refer [spawn fork]]
            [path :refer [normalize]]
            [electron :refer [app]]
            [http :as http]
            [cljs.nodejs :as nodejs :refer [process]]))


(def PORT 7788)

(defn jvm-up?
  [{:keys [put-fn] :as m}]
  (log/info "JVM up?")
  (let [try-again
        (fn [_]
          (log/info "- Nope, trying again")
          (put-fn [:window/loading])
          (put-fn [:cmd/schedule-new {:timeout 1000 :message [:jvm/loaded?]}]))
        res-handler
        (fn [res]
          (let [status-code (.-statusCode res)]
            (log/info "HTTP response: " status-code (= status-code 200))
            (if (= status-code 200)
              (put-fn [:window/new "main"])
              (try-again res))))
        req (http/get (clj->js {:host "localhost" :port 7788}) res-handler)]
    (.on req "error" try-again)
    {}))

(def runtime-info
  (let [user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (= "/" cwd)
                   (str rp "/app")
                   (str cwd))]
    {:platform       (.-platform process)
     :java-path      "/usr/bin/java"
     :data-path      (str user-data "/data")
     :jar-path       (str app-path "/bin/iwaswhere.jar")
     :blink-path     (str app-path "/bin/blink1-mac-cli")
     :user-data      user-data
     :cwd            cwd
     :resources-path rp
     :app-path       app-path}))

(defn start-jvm
  [{:keys [current-state]}]
  (let [platform (.-platform process)
        user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (= "/" cwd)
                   (str rp "/app")
                   (str cwd))
        jar-path (str app-path "/bin/iwaswhere.jar")
        blink-path (str app-path "/bin/blink1-mac-cli")
        data-path (str user-data "/data")
        java-path "/usr/bin/java"
        service (spawn java-path
                       (clj->js ["-Dapple.awt.UIElement=true"
                                 "-XX:+AggressiveOpts"
                                 "-jar"
                                 jar-path])
                       (clj->js {:detached false
                                 :cwd      user-data
                                 :env      {:PORT            PORT
                                            :DATA_PATH       data-path
                                            :BLINK_PATH      blink-path
                                            :CACHED_APPSTATE true}}))
        std-out (.-stdout service)
        std-err (.-stderr service)
        geocoder (fork (str app-path "/geocoder.js")
                       (clj->js [])
                       (clj->js {:cwd cwd}))
        spotify (fork (str app-path "spotify.js")
                      (clj->js [])
                      (clj->js {:cwd cwd
                                :env {:USER_DATA user-data}}))]
    (log/info "JVM: startup" platform)
    (log/info "JVM: jvm-path" jar-path)
    (log/info "JVM: cwd" cwd)
    (log/info "JVM: app-path" app-path)
    (log/info "JVM: user-data" user-data)
    (log/info "JVM: rp" rp)
    (.on std-out "data" #(log/info "JVM " (.toString % "utf8")))
    (.on std-err "data" #(log/error "JVM " (.toString % "utf8")))
    {}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:jvm/start   start-jvm
                 :jvm/loaded? jvm-up?}})

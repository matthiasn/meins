(ns iwaswhere-electron.main.startup
  (:require [iwaswhere-electron.main.log :as log]
            [child_process :refer [spawn fork]]
            [path :refer [normalize]]
            [electron :refer [app session]]
            [http :as http]
            [fs :refer [existsSync renameSync readFileSync]]
            [cljs.nodejs :as nodejs :refer [process]]
            [clojure.pprint :as pp]))


(def PORT 7788)

(def runtime-info
  (let [user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (= "/" cwd)
                   (str rp "/app")
                   (str cwd))
        info {:platform       (.-platform process)
              :java           (str app-path "/bin/zulu8.23.0.3-jdk8.0.144-mac_x64/bin/java")
              :data-path      (str user-data "/data")
              :cache          (str user-data "/data/cache.dat")
              :jar            (str app-path "/bin/iwaswhere.jar")
              :blink          (str app-path "/bin/blink1-mac-cli")
              :user-data      user-data
              :cwd            cwd
              :pid-file       (str user-data "/iwaswhere.pid")
              :resources-path rp
              :app-path       app-path}]
    (into {} (map (fn [[k v]] [k (normalize v)]) info))))


(defn jvm-up?
  [{:keys [put-fn current-state cmp-state]}]
  (log/info "JVM up?" (:attempt current-state) (with-out-str (pp/pprint runtime-info)))
  (let [try-again
        (fn [_]
          (log/info "- Nope, trying again")
          (when-not (:service @cmp-state)
            (put-fn [:cmd/schedule-new {:timeout 10 :message [:jvm/start]}]))
          (put-fn [:window/loading])
          (put-fn [:cmd/schedule-new {:timeout 1000 :message [:jvm/loaded?]}]))
        res-handler
        (fn [res]
          (let [status-code (.-statusCode res)]
            (log/info "HTTP response: " status-code (= status-code 200))
            (if (= status-code 200)
              (put-fn [:window/new "main"])
              (try-again res))))
        req (http/get (clj->js {:host "localhost" :port PORT}) res-handler)]
    (.on req "error" try-again)
    {:new-state (update-in current-state [:attempt] #(inc (or % 0)))}))

(defn spawn-process [cmd args opts]
  (spawn cmd (clj->js args) (clj->js opts)))

(defn start-jvm
  [{:keys [current-state]}]
  (let [{:keys [user-data app-path jar blink data-path java cwd]} runtime-info
        service (spawn-process java
                               ["-Dapple.awt.UIElement=true"
                                "-XX:+AggressiveOpts"
                                "-jar"
                                jar]
                               {:detached false
                                :cwd      user-data
                                :env      {:PORT            PORT
                                           :DATA_PATH       data-path
                                           :BLINK_PATH      blink
                                           :CACHED_APPSTATE true}})
        std-out (.-stdout service)
        std-err (.-stderr service)
        geocoder (fork (str app-path "/geocoder.js")
                       (clj->js [])
                       (clj->js {:detached true
                                 :cwd      cwd}))
        spotify (fork (str app-path "/spotify.js")
                      (clj->js [])
                      (clj->js {:cwd cwd
                                :env {:USER_DATA user-data}}))]
    (log/info "JVM: startup" (with-out-str (pp/pprint runtime-info)))
    (.on std-out "data" #(log/info "JVM " (.toString % "utf8")))
    (.on std-err "data" #(log/error "JVM " (.toString % "utf8")))
    {:new-state (assoc-in current-state [:service] service)}))

(defn shutdown
  [{:keys []}]
  (log/info "Shutting down")
  (.quit app)
  {})

(defn shutdown-jvm
  [{:keys [current-state]}]
  (log/info "Shutting down JVM service")
  (let [pid (readFileSync (:pid-file runtime-info) "utf-8")]
    (log/info "Shutting down JVM service" pid)
    (when pid
      (if (= (:platform runtime-info) "win32")
        (spawn-process "TaskKill" ["-F" "/PID" pid] {})
        (spawn-process "/bin/kill" ["-KILL" pid] {}))))
  {:send-to-self [:app/shutdown]})

(defn clear-cache
  [{:keys []}]
  (log/info "Clearing Electron Cache")
  (let [session (.-defaultSession session)]
    (.clearCache session #(log/info "Electron Cache Cleared")))
  {})

(defn clear-iww-cache
  [{:keys []}]
  (log/info "Clearing iWasWhere Cache")
  (let [cache-file (:cache runtime-info)
        cache-exists? (.existsSync fs cache-file)]
    (when cache-exists?
      (.renameSync fs cache-file (str cache-file ".bak"))))
  {})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:jvm/start           start-jvm
                 :jvm/loaded?         jvm-up?
                 :app/shutdown        shutdown
                 :app/shutdown-jvm    shutdown-jvm
                 :app/clear-iww-cache clear-iww-cache
                 :app/clear-cache     clear-cache}})

(.on app "window-all-closed"
     #(when-not (= (:platform runtime-info) "darwin")
        (.quit app)))

(ns iwaswhere-electron.renderer.ipc
  (:require [iwaswhere-electron.renderer.log :as log]
            [electron :refer [ipcRenderer]]
            [cljs.reader :refer [read-string]]))

(defn state-fn
  [put-fn]
  (let [cmd-handler (fn [ev msg]
                      (log/info "IPC in:" msg)
                      (put-fn [:exec/js msg]))
        relay-handler (fn [ev m]
                        (let [parsed (read-string m)
                              msg-type (first parsed)
                              {:keys [msg-payload msg-meta]} (second parsed)
                              msg (with-meta [msg-type msg-payload] msg-meta)]
                          (log/info "IPC relay in:" msg)
                          (put-fn msg)))]
    (.on ipcRenderer "cmd" cmd-handler)
    (.on ipcRenderer "relay" relay-handler)
    (log/info "Starting IPC Component")
    {:state (atom {})}))


(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

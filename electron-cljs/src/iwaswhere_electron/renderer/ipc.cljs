(ns iwaswhere-electron.renderer.ipc
  (:require [iwaswhere-electron.renderer.log :as log]
            [electron :refer [ipcRenderer]]))

(defn state-fn
  [put-fn]
  (let [cmd-handler (fn [ev msg]
                      (log/info "IPC in:" msg)
                      (put-fn [:exec/js msg]))]
    (.on ipcRenderer "cmd" cmd-handler)
    (log/info "Starting IPC Component")
    {:state (atom {})}))


(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

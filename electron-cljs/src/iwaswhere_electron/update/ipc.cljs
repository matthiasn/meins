(ns iwaswhere-electron.update.ipc
  (:require [iwaswhere-electron.update.log :as log]
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

(defn relay-msg
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
    (log/info "Relaying" (str msg-type) (str msg-payload))
    (.send ipcRenderer "relay" (pr-str serializable)))
  {})

(defn cmp-map
  [cmp-id relay-types]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map (zipmap relay-types (repeat relay-msg))})

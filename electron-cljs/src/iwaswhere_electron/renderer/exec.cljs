(ns iwaswhere-electron.renderer.exec
  (:require [iwaswhere-electron.renderer.log :as log]
            [electron :refer [ipcRenderer]]))

(defn state-fn [put-fn]
  (let [webview (.querySelector js/document "webview")
        web-contents (.getWebContents webview)]
    (log/info "Starting EXEC Component")
    {:state (atom {:web-contents web-contents})}))

(defn exec-js [{:keys [current-state msg-payload]}]
  (log/info "EXEC:" msg-payload)
  (let [wc (:web-contents current-state)]
    (when wc
      (.executeJavaScript wc msg-payload))
    {}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:exec/js exec-js}})

(ns iwaswhere-electron.renderer.core
  (:require [iwaswhere-electron.renderer.log :as log]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :electron/switchboard))


(defn cmd-handler [ev msg]
  (log/info "Received CMD:" msg)
  (let [webview (.querySelector js/document "webview")
        web-contents (.getWebContents webview)]
    (.executeJavaScript web-contents "iwaswhere_web.ui.menu.hide()")))


(.on ipcRenderer "cmd" cmd-handler)


(defn console-msg-handler [ev]
  (log/info "GUEST:" (.-message ev)))


(defn load-handler [ev]
  (log/info "Loaded")
  (let [webview (.querySelector js/document "webview")]
    (.addEventListener webview "console-message" console-msg-handler)))


(.addEventListener js/window "load" load-handler)


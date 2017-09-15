(ns iwaswhere-electron.renderer.core
  (:require [iwaswhere-electron.renderer.log]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [iwaswhere-electron.renderer.exec :as exec]
            [electron :refer [ipcRenderer shell]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [clojure.string :as s]
            [clojure.string :as s]))

(defonce switchboard (sb/component :renderer/switchboard))

(defn console-msg-handler [ev]
  (info "GUEST:" (.-message ev)))

(defn start []
  (info "Starting SYSTEM")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :renderer/ipc-cmp #{:app/open-external})
                       (exec/cmp-map :renderer/exec-cmp #{:import/listen
                                                          :firehose/cmp-put
                                                          :firehose/cmp-recv
                                                          :cmd/toggle-key})}]
     [:cmd/route {:from :renderer/ipc-cmp
                  :to   #{:renderer/exec-cmp}}]

     [:cmd/route {:from :renderer/exec-cmp
                  :to   #{:renderer/ipc-cmp}}]

     [:cmd/send {:to  :renderer/exec-cmp
                 :msg [:exec/js "iwaswhere_web.ui.menu.hide()"]}]]))

(defn load-handler [ev]
  (info "RENDERER loaded")
  (let [webview (.querySelector js/document "webview")]
    (.addEventListener webview "console-message" console-msg-handler)
    (start)))

(.addEventListener js/window "load" load-handler)

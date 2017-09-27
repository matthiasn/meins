(ns iwaswhere-electron.renderer.core
  (:require [iwaswhere-electron.renderer.log]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [iwaswhere-electron.renderer.exec :as exec]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :renderer/switchboard))

(defn console-msg-handler [ev]
  (info "GUEST:" (.-message ev)))

(def OBSERVER true)

(defn make-observable
      [components]
      (if OBSERVER
        (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
             (set (mapv mapper components)))
        components))

(defn start []
  (info "Starting SYSTEM")
  (let [components #{(ipc/cmp-map :renderer/ipc-cmp #{:app/open-external})
                     (exec/cmp-map :renderer/exec-cmp #{:import/listen
                                                        :firehose/cmp-put
                                                        :firehose/cmp-recv
                                                        :cmd/toggle-key})}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]
       [:cmd/route {:from :renderer/ipc-cmp
                    :to   #{:renderer/exec-cmp}}]

       [:cmd/route {:from :renderer/exec-cmp
                    :to   #{:renderer/ipc-cmp}}]

       (when OBSERVER
         [:cmd/attach-to-firehose :renderer/exec-cmp])

       [:cmd/send {:to  :renderer/exec-cmp
                   :msg [:exec/js {:js "iwaswhere_web.ui.menu.hide()"}]}]])))

(defn load-handler [ev]
  (info "RENDERER loaded")
  (let [webview (.querySelector js/document "webview")]
    (.addEventListener webview "console-message" console-msg-handler)
    (start)))

(.addEventListener js/window "load" load-handler)

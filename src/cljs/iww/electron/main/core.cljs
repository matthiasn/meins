(ns iww.electron.main.core
  (:require [iww.electron.main.log]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [matthiasn.systems-toolbox-electron.ipc-main :as ipc]
            [matthiasn.systems-toolbox-electron.window-manager :as wm]
            [iww.electron.main.menu :as menu]
            [iww.electron.main.update :as upd]
            [iww.electron.main.startup :as st]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]
            [iww.electron.main.runtime :as rt]))

(aset process "env" "GOOGLE_API_KEY" "AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU")

(defonce switchboard (sb/component :electron/switchboard))

(def OBSERVER true)

(defn make-observable
  [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (set (mapv mapper components)))
    components))

(def wm-relay #{:exec/js
                :cmd/toggle-key
                :firehose/cmp-put
                :firehose/cmp-recv
                :update/status
                :import/listen})

(def app-path (:app-path rt/runtime-info))

(defn start []
  (info "Starting CORE:" (.-resourcesPath process))
  (let [components #{(wm/cmp-map :electron/window-manager wm-relay app-path)
                     (st/cmp-map :electron/startup-cmp)
                     (ipc/cmp-map :electron/ipc-cmp)
                     (upd/cmp-map :electron/update-cmp)
                     (sched/cmp-map :electron/scheduler-cmp)
                     (menu/cmp-map :electron/menu-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :electron/menu-cmp
                    :to   #{:electron/window-manager
                            :electron/startup-cmp
                            :electron/scheduler-cmp
                            :electron/update-cmp}}]

       [:cmd/route {:from :electron/scheduler-cmp
                    :to   #{:electron/update-cmp
                            :electron/window-manager
                            :electron/startup-cmp}}]

       [:cmd/route {:from :electron/ipc-cmp
                    :to   #{:electron/startup-cmp
                            :electron/update-cmp
                            :electron/window-manager}}]

       [:cmd/route {:from :electron/window-manager
                    :to   :electron/startup-cmp}]

       [:cmd/route {:from :electron/update-cmp
                    :to   #{:electron/scheduler-cmp
                            :electron/window-manager
                            :electron/startup-cmp}}]

       [:cmd/route {:from :electron/startup-cmp
                    :to   #{:electron/scheduler-cmp
                            :electron/window-manager}}]

       (when OBSERVER
         [:cmd/attach-to-firehose :electron/window-manager])

       [:cmd/send {:to  :electron/startup-cmp
                   :msg [:jvm/loaded?]}]

       [:cmd/send {:to  :electron/scheduler-cmp
                   :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                            :message [:update/auto-check]
                                            :repeat  true
                                            :initial true}]}]])))

(.on app "ready" start)

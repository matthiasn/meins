(ns iww.electron.main.core
  (:require [iww.electron.main.log]
            [iwaswhere-web.specs]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [matthiasn.systems-toolbox-electron.ipc-main :as ipc]
            [matthiasn.systems-toolbox-electron.window-manager :as wm]
            [iww.electron.main.menu :as menu]
            [iww.electron.main.update :as upd]
            [iww.electron.main.geocoder :as geocoder]
            [iww.electron.main.startup :as st]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]
            [iww.electron.main.runtime :as rt]))

(aset process "env" "GOOGLE_API_KEY" "AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU")

(defonce switchboard (sb/component :electron/switchboard))

(def OBSERVER (:repo-dir rt/runtime-info))

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
                :screenshot/take
                :cmd/pomodoro-inc
                :geonames/res
                :spellcheck/lang
                :spellcheck/off
                :import/screenshot
                :import/listen})

(def app-path (:app-path rt/runtime-info))

(defn start []
  (info "Starting CORE:" (.-resourcesPath process))
  (info "download-path" (:downloads rt/runtime-info))
  (let [components #{(wm/cmp-map :electron/window-manager wm-relay app-path)
                     (st/cmp-map :electron/startup)
                     (ipc/cmp-map :electron/ipc-cmp)
                     (upd/cmp-map :electron/updater)
                     (sched/cmp-map :electron/scheduler)
                     (menu/cmp-map :electron/menu-cmp)
                     (geocoder/cmp-map :electron/geocoder #{:geonames/lookup})}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :electron/menu-cmp
                    :to   #{:electron/window-manager
                            :electron/startup
                            :electron/scheduler
                            :electron/geocoder
                            :electron/updater}}]

       [:cmd/route {:from :electron/scheduler
                    :to   #{:electron/updater
                            :electron/window-manager
                            :electron/geocoder
                            :electron/startup}}]

       [:cmd/route {:from :electron/ipc-cmp
                    :to   #{:electron/startup
                            :electron/updater
                            :electron/geocoder
                            :electron/scheduler
                            :electron/window-manager}}]

       [:cmd/route {:from :electron/window-manager
                    :to   :electron/startup}]

       [:cmd/route {:from :electron/geocoder
                    :to   :electron/window-manager}]

       [:cmd/route {:from :electron/updater
                    :to   #{:electron/scheduler
                            :electron/window-manager
                            :electron/startup}}]

       [:cmd/route {:from :electron/startup
                    :to   #{:electron/scheduler
                            :electron/window-manager}}]

       (when OBSERVER
         [:cmd/attach-to-firehose :electron/window-manager])

       [:cmd/send {:to  :electron/startup
                   :msg [:jvm/loaded?]}]

       [:cmd/send {:to  :electron/scheduler
                   :msg [:cmd/schedule-new {:message [:geocoder/start]
                                            :timeout 5000}]}]

       [:cmd/send {:to  :electron/scheduler
                   :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                            :message [:update/auto-check]
                                            :repeat  true
                                            :initial true}]}]])))

(.on app "ready" start)

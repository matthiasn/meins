(ns iwaswhere-electron.main.core
  (:require [iwaswhere-electron.main.log]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [iwaswhere-electron.main.menu :as menu]
            [iwaswhere-electron.main.update :as upd]
            [iwaswhere-electron.main.startup :as st]
            [iwaswhere-electron.main.window-manager :as wm]
            [iwaswhere-electron.main.update-window :as um]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(aset process "env" "GOOGLE_API_KEY" "AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU")

(defonce switchboard (sb/component :electron/switchboard))

(def OBSERVER true)

(defn make-observable
  [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (set (mapv mapper components)))
    components))

(defn start []
  (info "Starting CORE:" (.-resourcesPath process))
  (let [components #{(wm/cmp-map :electron/wm-cmp #{:exec/js
                                                    :cmd/toggle-key
                                                    :firehose/cmp-put
                                                    :firehose/cmp-recv
                                                    :import/listen})
                     (st/cmp-map :electron/startup-cmp)
                     (upd/cmp-map :electron/update-cmp)
                     (sched/cmp-map :electron/scheduler-cmp)
                     (um/cmp-map :electron/update-win-cmp)
                     (menu/cmp-map :electron/menu-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :electron/menu-cmp
                    :to   #{:electron/wm-cmp
                            :electron/update-win-cmp
                            :electron/startup-cmp
                            :electron/update-cmp}}]

       [:cmd/route {:from #{:electron/update-win-cmp
                            :electron/scheduler-cmp}
                    :to   #{:electron/update-cmp
                            :electron/startup-cmp}}]

       [:cmd/route {:from :electron/wm-cmp
                    :to   :electron/startup-cmp}]

       [:cmd/route {:from :electron/update-cmp
                    :to   #{:electron/update-win-cmp
                            :electron/scheduler-cmp}}]

       [:cmd/route {:from #{:electron/update-cmp
                            :electron/scheduler-cmp}
                    :to   :electron/startup-cmp}]

       [:cmd/route {:from :electron/startup-cmp
                    :to   #{:electron/scheduler-cmp
                            :electron/wm-cmp}}]

       (when OBSERVER
         [:cmd/attach-to-firehose :electron/wm-cmp])

       [:cmd/send {:to  :electron/startup-cmp
                   :msg [:jvm/loaded?]}]

       [:cmd/send {:to  :electron/scheduler-cmp
                   :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                            :message [:update/auto-check]
                                            :repeat  true
                                            :initial true}]}]])))

(.on app "ready" start)

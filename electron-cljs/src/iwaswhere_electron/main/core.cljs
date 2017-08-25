(ns iwaswhere-electron.main.core
  (:require [iwaswhere-electron.main.log :as log]
            [iwaswhere-electron.main.menu :as menu]
            [iwaswhere-electron.main.update :as upd]
            [iwaswhere-electron.main.window-manager :as wm]
            [iwaswhere-electron.main.update-window :as um]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(aset process "env" "GOOGLE_API_KEY" "AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU")

(defonce switchboard (sb/component :electron/switchboard))

(defn start []
  (log/info "Starting CORE:" (.-resourcesPath process))
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(wm/cmp-map :electron/wm-cmp #{:exec/js})
                       (upd/cmp-map :electron/update-cmp)
                       (sched/cmp-map :electron/scheduler-cmp)
                       (um/cmp-map :electron/update-win-cmp)
                       (menu/cmp-map :electron/menu-cmp)}]

     [:cmd/route {:from :electron/menu-cmp
                  :to   #{:electron/wm-cmp
                          :electron/update-win-cmp
                          :electron/update-cmp}}]

     [:cmd/route {:from #{:electron/update-win-cmp
                          :electron/scheduler-cmp}
                  :to   :electron/update-cmp}]

     [:cmd/route {:from :electron/update-cmp
                  :to   :electron/update-win-cmp}]

     [:cmd/send {:to  :electron/wm-cmp
                 :msg [:window/new "main"]}]

     [:cmd/send {:to  :electron/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                          :message [:update/auto-check]
                                          :repeat  true
                                          :initial true}]}]]))

(.on app "ready" start)

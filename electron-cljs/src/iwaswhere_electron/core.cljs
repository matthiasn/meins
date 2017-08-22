(ns iwaswhere-electron.core
  (:require [iwaswhere-electron.log :as log]
            [iwaswhere-electron.menu :as menu]
            [iwaswhere-electron.window-manager :as wm]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(aset process "env" "GOOGLE_API_KEY" "AIzaSyD78NTnhgt--LCGBdIGPEg8GtBYzQl0gKU")

(defonce switchboard (sb/component :electron/switchboard))

(defn start []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(wm/cmp-map :electron/wm-cmp)
                       (menu/cmp-map :electron/menu-cmp)}]

     [:cmd/route {:from :electron/menu-cmp
                  :to   #{:electron/wm-cmp}}]

     [:cmd/send {:to  :electron/wm-cmp
                 :msg [:window/new "main"]}]]))

(.on app "ready" start)

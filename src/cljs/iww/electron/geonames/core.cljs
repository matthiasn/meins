(ns iww.electron.geonames.core
  (:require [iww.electron.geonames.log]
            [electron-log :as l]
            [iww.electron.geonames.geonames :as geonames]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [electron :refer [app]]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(nodejs/enable-util-print!)

(defonce switchboard (sb/component :geonames/switchboard))

(def OBSERVER true)

(defn make-observable
  [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (set (mapv mapper components)))
    components))

(defn start []
  (info "Starting geonames CORE")
  (let [components #{(geonames/cmp-map :geonames/service)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]
       #_
       [:cmd/route {:from :electron/menu-cmp
                    :to   #{:electron/window-manager
                            :electron/startup-cmp
                            :electron/scheduler-cmp
                            :electron/update-cmp}}]
       #_
       (when OBSERVER
         [:cmd/attach-to-firehose :electron/window-manager])
       ])))

(start)

(ns iwaswhere-web.core
  (:require [iwaswhere-web.specs]
            [iwaswhere-web.client-store :as store]
            [iwaswhere-web.ui.re-frame :as rf]
            [iwaswhere-web.router :as router]
            [iwaswhere-web.keepalive :as ka]
            [re-frisk.core :as frisk]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(enable-console-print!)
;(frisk/enable-re-frisk!)

(defonce switchboard (sb/component :client/switchboard))

(def sente-cfg {:relay-types #{:entry/update :entry/find :entry/trash
                               :import/geo :import/photos :import/phone
                               :import/spotify :import/flight :export/pdf
                               :cmd/keep-alive :stats/pomo-day-get
                               :stats/get :stats/get2 :import/movie :blink/busy
                               :state/stats-tags-get :import/weight :import/listen
                               :state/search :cfg/refresh}})

(defn init!
  "Initializes client-side system by sending messages to the switchboard for
   initializing and then wiring components. Finally, a call to init-keepalive!
   starts the connection keepalive functionality."
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(sente/cmp-map :client/ws-cmp sente-cfg)
                       (store/cmp-map :client/store-cmp)
                       (router/cmp-map :client/router-cmp)
                       (sched/cmp-map :client/scheduler-cmp)
                       (rf/cmp-map :client/ui-cmp)}]

     [:cmd/route {:from #{:client/store-cmp
                          :client/ui-cmp}
                  :to   :client/ws-cmp}]

     [:cmd/route {:from #{:client/ws-cmp
                          :client/ui-cmp
                          :client/router-cmp}
                  :to   :client/store-cmp}]

     [:cmd/route {:from #{:client/store-cmp
                          :client/ui-cmp}
                  :to   #{:client/scheduler-cmp
                          :client/router-cmp}}]

     [:cmd/observe-state {:from :client/store-cmp
                          :to   :client/ui-cmp}]

     [:cmd/route {:from :client/scheduler-cmp
                  :to   #{:client/store-cmp
                          :client/ws-cmp}}]])
  (ka/init-keepalive! switchboard))

(init!)


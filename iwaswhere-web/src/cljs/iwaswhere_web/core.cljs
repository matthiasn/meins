(ns iwaswhere-web.core
  (:require [iwaswhere-web.specs]
            [iwaswhere-web.client-store :as store]
            [iwaswhere-web.ui.stats :as stats]
            [iwaswhere-web.ui.menu :as m]
            [iwaswhere-web.ui.content :as c]
            [iwaswhere-web.keepalive :as ka]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(def sente-cfg {:relay-types #{:state/get :entry/update :entry/find :entry/trash
                               :import/geo :import/photos :import/phone
                               :cmd/keep-alive :stats/pomo-day-get
                               :stats/activity-day-get :stats/tasks-day-get
                               :state/stats-tags-get :import/weight}})

(defn init!
  "Initializes client-side system by sending messages to the switchboard for
   initializing and then wiring components. Finally, a call to init-keepalive!
   starts the connection keepalive functionality."
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp
      #{(sente/cmp-map :client/ws-cmp sente-cfg) ; WebSocket communication
        (m/cmp-map :client/menu-cmp)             ; UI component for menu
        (c/cmp-map :client/content-cmp)          ; UI component for journal
        (store/cmp-map :client/store-cmp)        ; Data store component
        (sched/cmp-map :client/scheduler-cmp)    ; Scheduler component
        (stats/cmp-map :client/stats-cmp)        ; UI component for stats
        }]

     [:cmd/route {:from #{:client/store-cmp
                          :client/stats-cmp
                          :client/content-cmp
                          :client/menu-cmp}
                  :to   :client/ws-cmp}]

     [:cmd/route {:from #{:client/ws-cmp
                          :client/content-cmp
                          :client/menu-cmp}
                  :to   :client/store-cmp}]

     [:cmd/observe-state {:from :client/store-cmp
                          :to   #{:client/content-cmp
                                  :client/menu-cmp
                                  :client/stats-cmp}}]

     [:cmd/route {:from :client/ws-cmp
                  :to   :client/stats-cmp}]

     [:cmd/route {:from :client/store-cmp
                  :to   :client/scheduler-cmp}]

     [:cmd/route {:from :client/scheduler-cmp
                  :to   #{:client/store-cmp
                          :client/ws-cmp}}]])
  (ka/init-keepalive! switchboard))

(init!)


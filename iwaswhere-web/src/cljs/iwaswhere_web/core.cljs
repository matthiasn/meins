(ns iwaswhere-web.core
  (:require [iwaswhere-web.store :as store]
            [iwaswhere-web.ui.new-entry :as ne]
            [iwaswhere-web.ui.search :as s]
            [iwaswhere-web.ui.journal :as jrn]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init
  [client-ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp client-ws-cmp]                         ; WebSocket communication component
     [:cmd/init-comp (ne/cmp-map :client/new-entry-cmp)]    ; UI component for new journal entries
     [:cmd/init-comp (s/cmp-map :client/search-cmp)]        ; UI component for new journal entries
     [:cmd/init-comp (jrn/cmp-map :client/journal-cmp)]     ; UI component for journal
     [:cmd/init-comp (store/cmp-map :client/store-cmp)]     ; Data store component

     [:cmd/route-all {:from [:client/store-cmp :client/new-entry-cmp :client/search-cmp :client/journal-cmp]
                      :to   :client/ws-cmp}]

     [:cmd/route {:from [:client/ws-cmp
                         :client/new-entry-cmp
                         :client/search-cmp
                         :client/journal-cmp]
                  :to   :client/store-cmp}]

     [:cmd/observe-state {:from :client/store-cmp
                          :to   [:client/journal-cmp
                                 :client/search-cmp]}]

     [:cmd/init-comp (sched/cmp-map :client/scheduler-cmp)]  ; Scheduler component
     [:cmd/send {:to  :client/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/keep-alive]
                                          :repeat true
                                          :initial true}]}]
     [:cmd/route-all {:from :client/scheduler-cmp
                      :to   :client/ws-cmp}]]))

(init (sente/cmp-map :client/ws-cmp))

(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defn init-keepalive!
  "Here, messages to keep the connection alive are sent to the backend every 5 seconds."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :client/scheduler-cmp)]  ; Scheduler component
     [:cmd/send {:to  :client/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/keep-alive]
                                          :repeat true
                                          :initial true}]}]
     [:cmd/route-all {:from :client/scheduler-cmp :to :client/ws-cmp}]]))

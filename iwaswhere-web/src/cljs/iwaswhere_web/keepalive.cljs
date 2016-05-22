(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defn init-keepalive!
  "Here, messages to keep the connection alive are sent to the backend every second."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :client/scheduler-cmp)]  ; Scheduler component
     [:cmd/send {:to  :client/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 1000
                                          :message [:cmd/keep-alive]
                                          :repeat true
                                          :initial false}]}]
     [:cmd/route-all {:from :client/scheduler-cmp :to :client/ws-cmp}]
     [:cmd/route {:from :client/scheduler-cmp :to :client/store-cmp}]]))

(defn set-alive-fn
  "Set :last-alive key whenever a keepalive response message was received by the backend."
  [{:keys [current-state]}]
  {:new-state (assoc-in current-state [:last-alive] (.now js/Date))})

(defn reset-fn
  "Reset local state when last message from backend was seen more than 2 seconds ago."
  [{:keys [current-state]}]
  (when (> (- (.now js/Date) (:last-alive current-state)) 5000)
    {:new-state {}}))
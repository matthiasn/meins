(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as st]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [clojure.pprint :as pp]))

;; Server side
(defn restart-keepalive!
  "Starts or restarts connection-gc part of system. Here, messages to start
   garbage collecting queries from clients that have not been seen in a while
   are sent to the store-cmp."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]
     [:cmd/route {:from :server/scheduler-cmp
                  :to   #{:server/store-cmp :server/ws-cmp}}]
     [:cmd/route {:from :server/store-cmp :to :server/scheduler-cmp}]]))

(defn keepalive-fn
  "Responds to keepalive message."
  [{:keys []}]
  {:emit-msg [:cmd/keep-alive-res]})

(def max-age 15000)

;; Client side
(defn init-keepalive!
  "Here, messages to keep the connection alive are sent to the backend every
   second."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/send {:to  :client/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/keep-alive]
                                          :repeat true
                                          :initial false}]}]]))

(defn set-alive-fn
  "Sets :last-alive key whenever a keepalive response message was received from
   the backend."
  [{:keys [current-state]}]
  {:new-state (assoc-in current-state [:last-alive] (st/now))})

(defn reset-fn
  "Reset local state when last message from backend was seen more than 10
   seconds ago."
  [{:keys [current-state]}]
  (when (> (- (st/now) (:last-alive current-state)) max-age)
    {:new-state (-> current-state
                    (assoc-in [:results] {})
                    (assoc-in [:entries-map] {}))}))

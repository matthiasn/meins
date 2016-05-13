(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defn restart-keepalive!
  "Starts or restarts connection-gc part of system. Here, messages to start garbage collecting
  queries from clients that have not been seen in a while are sent to the store-cmp."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/query-gc]
                                          :repeat true
                                          :initial true}]}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/store-cmp}]]))

(defn keepalive-fn
  "Mark client in the stored queries as recently seen to prevent it from being garbage collected."
  [{:keys [current-state msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)
        new-state (assoc-in current-state [:last-filter sente-uid :last-seen] (System/currentTimeMillis))]
    {:new-state new-state}))

(defn query-gc-fn
  "Garbage collect queries whose corresponding client has not recently sent a keepalive message."
  [{:keys [current-state]}]
  (let [last-filters (:last-filter current-state)
        last-acceptable-ts (- (System/currentTimeMillis) 10000)
        alive-filters (into {} (filter (fn [[k v]] (> (:last-seen v) last-acceptable-ts)) last-filters))
        new-state (assoc-in current-state [:last-filter] alive-filters)]
    {:new-state new-state}))

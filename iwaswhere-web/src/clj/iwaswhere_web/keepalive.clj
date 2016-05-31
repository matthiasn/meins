(ns iwaswhere-web.keepalive
  "This namespace is concerned with the server side of the connection keepalive mechanism."
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
  "Mark client in the stored queries as recently seen to prevent it from being garbage collected.
  Only returns new state when the query already exists in current state."
  [{:keys [current-state msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)
        new-state (assoc-in current-state [:client-queries sente-uid :last-seen] (System/currentTimeMillis))]
    (when (contains? (:client-queries current-state) sente-uid)
      {:emit-msg  (with-meta [:cmd/keep-alive-res] msg-meta)
       :new-state new-state})))

(def max-age 10000)

(defn query-gc-fn
  "Garbage collect queries whose corresponding client has not recently sent a keepalive message."
  [{:keys [current-state]}]
  (let [client-queries (:client-queries current-state)
        last-acceptable-ts (- (System/currentTimeMillis) max-age)
        alive-filters (into {} (filter (fn [[_k v]] (> (:last-seen v) last-acceptable-ts)) client-queries))
        new-state (assoc-in current-state [:client-queries] alive-filters)]
    {:new-state new-state}))

(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as st]
            [matthiasn.systems-toolbox.scheduler :as sched]))

;; Server side
(defn restart-keepalive!
  "Starts or restarts connection-gc part of system. Here, messages to start
   garbage collecting queries from clients that have not been seen in a while
   are sent to the store-cmp."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/query-gc]
                                          :repeat true
                                          :initial true}]}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/store-cmp}]
     [:cmd/route {:from :server/store-cmp :to :server/scheduler-cmp}]]))

(defn keepalive-fn
  "Mark client in the stored queries as recently seen to prevent it from being
   garbage collected.
   Only returns new state when the query already exists in current state."
  [{:keys [current-state msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)
        new-state (assoc-in current-state [:client-queries sente-uid :last-seen]
                            (st/now))]
    (when (contains? (:client-queries current-state) sente-uid)
      {:emit-msg  (with-meta [:cmd/keep-alive-res] msg-meta)
       :new-state new-state})))

(def max-age 15000)

(defn query-gc-fn
  "Garbage collect queries whose corresponding client has not recently sent a
   keepalive message."
  [{:keys [current-state]}]
  (let [client-queries (:client-queries current-state)
        last-acceptable-ts (- (st/now) max-age)
        filter-fn (fn [[_k v]]
                    (when-let [last-seen (:last-seen v)]
                      (> last-seen last-acceptable-ts)))
        alive-filters (into {} (filter filter-fn client-queries))
        new-state (assoc-in current-state [:client-queries] alive-filters)]
    {:new-state new-state}))


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
  "Set :last-alive key whenever a keepalive response message was received by
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

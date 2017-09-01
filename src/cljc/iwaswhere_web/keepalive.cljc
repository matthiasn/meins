(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.pprint :as pp]))

(defn keepalive-fn
  "Responds to keepalive message."
  [{:keys []}]
  {:emit-msg [:cmd/keep-alive-res]})

; Probably not all that useful until there's a login status, and then
; it should be configurable. For now: ten minutes
; TODO: rethink
(def max-age (* 10 60 1000))

;; Client side
(defn init-keepalive!
  "Here, messages to keep the connection alive are sent to the backend every
   second."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/send {:to  :client/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 10000
                                          :message [:cmd/keep-alive]
                                          :repeat true
                                          :initial false}]}]]))

(defn set-alive-fn
  "Sets :last-alive key whenever a keepalive response message was received from
   the backend."
  [{:keys [current-state]}]
  (let [now (st/now)
        busy? (when-let [last-busy (:last-busy current-state)]
                (< (- now last-busy) 1000))
        new-state (-> current-state
                      (assoc-in [:last-alive] now)
                      (assoc-in [:busy] busy?))]
    {:new-state new-state}))

(defn reset-fn
  "Reset local state when last message from backend was seen more than 10
   seconds ago."
  [{:keys [current-state]}]
  (when (> (- (st/now) (:last-alive current-state)) max-age)
    {:new-state (-> current-state
                    (assoc-in [:results] {})
                    (assoc-in [:entries-map] {}))}))

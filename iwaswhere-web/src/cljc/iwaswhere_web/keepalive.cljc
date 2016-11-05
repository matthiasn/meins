(ns iwaswhere-web.keepalive
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as st]
            [clojure.pprint :as pp]))

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

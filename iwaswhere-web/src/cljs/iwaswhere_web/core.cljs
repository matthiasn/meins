(ns iwaswhere-web.core
  (:require [iwaswhere-web.store :as store]
            [iwaswhere-web.new-entry :as ne]
            [iwaswhere-web.journal :as jrn]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init
  [client-ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp client-ws-cmp]                         ; WebSocket communication component
     [:cmd/init-comp (ne/cmp-map :client/new-entry-cmp)]    ; UI component for new journal entries
     [:cmd/init-comp (jrn/cmp-map :client/journal-cmp)]     ; UI component for journal
     [:cmd/init-comp (store/cmp-map :client/store-cmp)]     ; Data store component

     ;; Then, messages of a given type are wired from one component to another
     [:cmd/route-all {:from [:client/store-cmp :client/new-entry-cmp :client/journal-cmp]
                      :to   :client/ws-cmp}]
     [:cmd/route {:from :client/ws-cmp :to :client/store-cmp}]
     [:cmd/route {:from :client/new-entry-cmp :to :client/store-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to :client/journal-cmp}]]))

(init (sente/cmp-map :client/ws-cmp))

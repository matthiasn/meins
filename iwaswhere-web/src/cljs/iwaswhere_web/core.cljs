(ns iwaswhere-web.core
  (:require [iwaswhere-web.store :as store]
            [iwaswhere-web.ui.search :as s]
            [iwaswhere-web.ui.journal :as jrn]
            [iwaswhere-web.keepalive :as ka]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init!
  "Initializes client-side system by sending messages to the switchboard for initializing and then
  wiring components. Finally, a call to init-keepalive! starts the connection keepalive functionality."
  [client-ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp client-ws-cmp]                         ; WebSocket communication component
     [:cmd/init-comp (s/cmp-map :client/search-cmp)]        ; UI component for search, new, import
     [:cmd/init-comp (jrn/cmp-map :client/journal-cmp)]     ; UI component for journal
     [:cmd/init-comp (store/cmp-map :client/store-cmp)]     ; Data store component
     [:cmd/route-all {:from [:client/store-cmp :client/search-cmp :client/journal-cmp] :to :client/ws-cmp}]
     [:cmd/route {:from [:client/ws-cmp :client/search-cmp :client/journal-cmp] :to :client/store-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to [:client/journal-cmp :client/search-cmp]}]])
  (ka/init-keepalive! switchboard))

(init! (sente/cmp-map :client/ws-cmp))

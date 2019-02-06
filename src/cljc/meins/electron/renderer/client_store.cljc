(ns meins.electron.renderer.client-store
  (:require #?(:cljs [reagent.core :refer [atom]])
            #?(:clj  [taoensso.timbre :refer [info debug]]
               :cljs [taoensso.timbre :refer-macros [info debug]])
            [meins.electron.renderer.client-store.entry :as cse]
            [meins.electron.renderer.client-store.search :as s]
            [meins.electron.renderer.client-store.initial :as csi]
            [meins.electron.renderer.client-store.handlers :as csh]
            [meins.electron.renderer.client-store.cfg :as c]))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    csi/initial-state-fn
   :state-spec  :state/client-store-spec
   :handler-map (merge cse/entry-handler-map
                       s/search-handler-map
                       {:cfg/save         c/save-cfg
                        :gql/res          csh/gql-res
                        :gql/res2         csh/gql-res2
                        :gql/remove       csh/gql-remove
                        :startup/progress csh/progress
                        :startup/query    csi/initial-queries
                        :ws/ping          csh/ping
                        :backend-cfg/new  csh/save-backend-cfg
                        :nav/to           csh/nav-handler
                        :blink/busy       csh/blink-busy
                        :imap/status      csh/imap-status
                        :imap/cfg         csh/imap-cfg
                        :cfg/show-qr      c/show-qr-code
                        :cal/to-day       c/cal-to-day
                        :cmd/toggle       c/toggle-set-fn
                        :cmd/set-opt      c/set-conj-fn
                        :metrics/info     csh/save-metrics
                        :update/status    csh/set-updater-status
                        :cmd/set-dragged  c/set-currently-dragged
                        :cmd/toggle-key   c/toggle-key-fn
                        :cmd/assoc-in     c/assoc-in-state})})

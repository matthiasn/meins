(ns iwaswhere-web.store)

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [put-fn] (put-fn [:state/get {}]) {:state (atom {})})
   :handler-map {:state/new (fn [{:keys [msg-payload]}] {:new-state msg-payload})}})

(ns iwaswhere-web.store)

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:state/new (fn [{:keys [msg-payload]}] {:new-state msg-payload})}})

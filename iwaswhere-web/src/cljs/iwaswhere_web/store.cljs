(ns iwaswhere-web.store)

(defn entry-save-fn
  [{:keys [current-state msg-payload]}]
  {:new-state (assoc-in current-state [:new-entry] msg-payload)})

(defn new-state-fn
  [{:keys [current-state msg-payload]}]
  {:new-state (assoc-in current-state [:entries] (:entries msg-payload))})

(defn initial-state-fn
  "Creates the initial component state atom."
  [put-fn]
  (put-fn [:state/get {}])
  {:state (atom {:entries []
                 :new-entry {}})})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    initial-state-fn
   :handler-map {:state/new       new-state-fn
                 :text-entry/save entry-save-fn}})

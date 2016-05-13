(ns iwaswhere-web.store
  (:require [iwaswhere-web.helpers :as h]))

(defn new-state-fn
  "Update client side state with list of journal entries received from backend."
  [{:keys [current-state msg-payload]}]
  {:new-state (-> current-state
                  (assoc-in [:entries] (:entries msg-payload))
                  (assoc-in [:stats] (:stats msg-payload))
                  (assoc-in [:hashtags] (:hashtags msg-payload))
                  (assoc-in [:mentions] (:mentions msg-payload)))})

(defn initial-state-fn
  "Creates the initial component state atom. Holds a list of entries from the backend,
  a map with temporary entries that are being edited but not saved yet, and sets that
  contain information for which entries to show the map, or the edit mode."
  [_put-fn]
  {:state (atom {:entries       []
                 :show-maps-for #{}
                 :temp-entries  {}})})

(defn toggle-set
  "Toggles for example the visibility of a map or the edit mode for an individual
  journal entry. Requires the key to exist on the application state as a set."
  [{:keys [current-state msg-payload]}]
  (let [k (:key msg-payload)
        timestamp (:timestamp msg-payload)
        new-state (if (contains? (k current-state) timestamp)
                    (update-in current-state [k] disj timestamp)
                    (update-in current-state [k] conj timestamp))]
    {:new-state new-state}))

(defn cmp-map
  "Creates map for the component which holds the client-side application state."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    initial-state-fn
   :handler-map {:state/new          new-state-fn
                 :cmd/toggle         toggle-set}})

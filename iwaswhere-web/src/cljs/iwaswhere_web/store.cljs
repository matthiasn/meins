(ns iwaswhere-web.store)

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
  [put-fn]
  (put-fn [:state/get {}])
  {:state (atom {:entries       []
                 :show-maps-for #{}
                 :show-edit-for #{}
                 :new-entry-tmp {}
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

(defn update-temp-entry
  "Handler function, receives updated/modified entry and stores it in :temp-entries
  of the application state under the timestamp key."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [timestamp updated]} msg-payload
        new-state (assoc-in current-state [:temp-entries timestamp] updated)]
    {:new-state new-state}))

(defn save-temp-entry
  "Handler function for storing new entry being written. Useful e.g. for filtering
  hashtags."
  [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:new-entry-tmp] msg-payload)]
    {:new-state new-state}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    initial-state-fn
   :handler-map {:state/new          new-state-fn
                 :cmd/toggle         toggle-set
                 :new-entry/tmp-save save-temp-entry
                 :update/temp-entry  update-temp-entry}})

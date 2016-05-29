(ns iwaswhere-web.store
  (:require [alandipert.storage-atom :refer [local-storage]]
            [iwaswhere-web.keepalive :as ka]))

(defn new-state-fn
  "Update client side state with list of journal entries received from backend."
  [{:keys [current-state msg-payload]}]
  (let [entries-map (into {} (map (fn [entry] [(:timestamp entry) entry]) (:entries msg-payload)))
        new-state (-> current-state
                      (assoc-in [:entries] (:entries msg-payload))
                      (assoc-in [:entries-map] entries-map)
                      (assoc-in [:cfg :hashtags] (:hashtags msg-payload))
                      (assoc-in [:stats] (:stats msg-payload))
                      (assoc-in [:cfg :mentions] (:mentions msg-payload)))]
    {:new-state new-state}))

(defonce new-entries-ls (local-storage (atom {}) "iWasWhere_new_entries"))

(defn update-local-storage
  "Updates local-storage with the provided new-entries."
  [new-entries]
  (reset! new-entries-ls (:new-entries new-entries)))

(defn initial-state-fn
  "Creates the initial component state atom. Holds a list of entries from the backend,
  a map with temporary entries that are being edited but not saved yet, and sets that
  contain information for which entries to show the map, or the edit mode."
  [_put-fn]
  (let [initial-state (atom {:entries         []
                             :last-alive      (.now js/Date)
                             :show-entries    20
                             :new-entries     @new-entries-ls
                             :cfg {:show-maps-for   #{}
                                   :sort-by-upvotes false
                                   :show-all-maps   false
                                   :show-hashtags   true
                                   :show-context    true
                                   :show-pvt        false}})]
    {:state initial-state}))

(defn toggle-set-fn
  "Toggles for example the visibility of a map or the edit mode for an individual
  journal entry. Requires the key to exist on the application state as a set."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)
        timestamp (:timestamp msg-payload)
        new-state (if (contains? (get-in current-state path) timestamp)
                    (update-in current-state path disj timestamp)
                    (update-in current-state path conj timestamp))]
    {:new-state new-state}))

(defn toggle-key-fn
  "Toggles config key."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)]
    {:new-state (update-in current-state path not)}))

(defn new-entry-fn
  "Create locally stored new entry for further edit."
  [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:new-entries (:timestamp msg-payload)] msg-payload)]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn geo-enrich-fn
  "Enrich locally stored new entry with geolocation once it becomes available."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (update-in current-state [:new-entries ts] #(merge msg-payload %))]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn entry-saved-fn
  "Remove new entry from local when saving is confirmed by backend."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (update-in current-state [:new-entries] dissoc ts)]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn update-local-fn
  "Update locally stored new entry changes from edit element."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        saved-entry (get-in current-state [:new-entries ts])
        new-state (assoc-in current-state [:new-entries ts] (merge saved-entry msg-payload))]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn remove-local-fn
  "Remove new entry from local when saving is confirmed by backend."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (update-in current-state [:new-entries] dissoc ts)]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked entries view."
  [{:keys [current-state msg-payload]}]
  (let [new-state (-> current-state
                    (assoc-in [:current-query] msg-payload)
                    (assoc-in [:active] nil))]
    {:new-state new-state}))

(defn show-more-fn
  "Runs previous query but with more results. Also updates the number to show in the UI."
  [{:keys [current-state]}]
  (let [current-query (:current-query current-state)
        new-query (update-in current-query [:n] + 20)
        new-state (-> current-state
                      (assoc-in [:show-entries] (:n current-query))
                      (assoc-in [:current-query] new-query))]
    {:new-state new-state
     :emit-msg [:state/get new-query]}))

(defn set-active-fn
  "Sets entry in payload as the active entry for which to show linked entries."
  [{:keys [current-state msg-payload]}]
  {:new-state (assoc-in current-state [:active] msg-payload)})

(defn cmp-map
  "Creates map for the component which holds the client-side application state."
  [cmp-id]
  {:cmp-id            cmp-id
   :state-fn          initial-state-fn
   :snapshot-xform-fn #(dissoc % :last-alive)
   :handler-map       {:state/new          new-state-fn
                       :state/get          update-query-fn
                       :show/more          show-more-fn
                       :entry/new          new-entry-fn
                       :entry/geo-enrich   geo-enrich-fn
                       :entry/update-local update-local-fn
                       :entry/remove-local remove-local-fn
                       :entry/saved        entry-saved-fn
                       :cmd/set-active     set-active-fn
                       :cmd/toggle         toggle-set-fn
                       :cmd/toggle-key     toggle-key-fn
                       :cmd/keep-alive     ka/reset-fn
                       :cmd/keep-alive-res ka/set-alive-fn}})

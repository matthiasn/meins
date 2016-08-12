(ns iwaswhere-web.client-store-entry
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    [matthiasn.systems-toolbox.component :as st]))

#?(:clj  (defonce new-entries-ls (atom {}))
   :cljs (defonce new-entries-ls (local-storage (atom {}) "iWasWhere_new_entries")))

(defn update-local-storage
  "Updates local-storage with the provided new-entries."
  [new-entries]
  (reset! new-entries-ls (:new-entries new-entries)))

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
  "Enrich locally stored new entry with geolocation once it becomes available.
   Does nothing when entry is already saved in backend."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        local-entry (get-in current-state [:new-entries ts])
        new-state (update-in current-state [:new-entries ts] #(merge msg-payload %))]
    (when local-entry
      (update-local-storage new-state)
      {:new-state new-state})))

(defn entry-saved-fn
  "Remove new entry from local when saving is confirmed by backend."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        curr-local (get-in current-state [:new-entries ts])]
    (when (= (:md curr-local) (:md msg-payload))
      (let [new-state
            (-> current-state
                (update-in [:new-entries] dissoc ts)
                (assoc-in [:entries-map ts] (merge curr-local msg-payload)))]
        (update-local-storage new-state)
        {:new-state new-state}))))

(defn play-audio
  "Start playing audio element with provided DOM id."
  [id]
  #?(:cljs (.play (.getElementById js/document id))))

(defn pomodoro-inc-fn
  "Increments completed time of entry. Plays next tick sound and schedules a new increment
  message. Finally plays completion sound."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (update-in current-state [:new-entries ts :completed-time] inc)]
    (when (get-in current-state [:new-entries ts])
      (let [new-entry (get-in new-state [:new-entries ts])
            done? (= (:planned-dur new-entry) (:completed-time new-entry))]
        (if (:pomodoro-running new-entry)
          (do (when-not (:mute (:cfg current-state))
                (if done? (play-audio "ringer")
                          (play-audio "ticking-clock")))
              (update-local-storage new-state)
              {:new-state new-state
               :emit-msg  (when (not done?)
                            [:cmd/schedule-new
                             {:timeout 1000
                              :message [:cmd/pomodoro-inc {:timestamp ts}]}])})
          {:new-state current-state})))))

(defn pomodoro-start-fn
  "Start pomodoro for entry. Will toggle the :pomodoro-running status of the
   entry and schedule an initial increment message."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (if (get-in current-state [:new-entries ts :pomodoro-running])
                    (update-in current-state [:new-entries ts :interruptions] inc)
                    current-state)
        new-state (update-in new-state [:new-entries ts :pomodoro-running] not)]
    (when (get-in current-state [:new-entries ts])
      (update-local-storage new-state)
      {:new-state    new-state
       :send-to-self [:cmd/pomodoro-inc {:timestamp ts}]})))

(defn update-local-fn
  "Update locally stored new entry with changes from edit element."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        saved-entry (get-in current-state [:new-entries ts])
        new-state (assoc-in current-state [:new-entries ts]
                            (merge saved-entry msg-payload))]
    (update-local-storage new-state)
    {:new-state new-state}))

(defn remove-local-fn
  "Remove new entry from local when saving is confirmed by backend."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (update-in current-state [:new-entries] dissoc ts)]
    (update-local-storage new-state)
    {:new-state new-state}))

(def entry-handler-map
  {:entry/new          new-entry-fn
   :entry/geo-enrich   geo-enrich-fn
   :entry/update-local update-local-fn
   :entry/remove-local remove-local-fn
   :entry/saved        entry-saved-fn
   :cmd/pomodoro-inc   pomodoro-inc-fn
   :cmd/pomodoro-start pomodoro-start-fn})

(ns iwaswhere-web.client-store
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    [matthiasn.systems-toolbox.component :as st]
    [iwaswhere-web.keepalive :as ka]
    [iwaswhere-web.client-store-entry :as cse]
    [iwaswhere-web.client-store-search :as s]))

(defn new-state-fn
  "Update client side state with list of journal entries received from backend."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [query-id (:query-id msg-payload)
        store-meta (:client/store-cmp msg-meta)
        entries (:entries msg-payload)
        new-state (-> current-state
                      (assoc-in [:results query-id :entries] entries)
                      (update-in [:entries-map] merge (:entries-map msg-payload))
                      (assoc-in [:timing] {:query (:duration-ms msg-payload)
                                           :rtt   (- (:in-ts store-meta)
                                                     (:out-ts store-meta))}))]
    {:new-state new-state}))

(defn stats-tags-fn
  "Update client side state with stats and tags received from backend."
  [{:keys [current-state msg-payload]}]
  (let [new-state (-> current-state
                      (assoc-in [:cfg :hashtags] (:hashtags msg-payload))
                      (assoc-in [:cfg :pvt-hashtags] (:pvt-hashtags msg-payload))
                      (assoc-in [:cfg :pvt-displayed] (:pvt-displayed msg-payload))
                      (assoc-in [:stats] (:stats msg-payload))
                      (assoc-in [:cfg :activities] (:activities msg-payload))
                      (assoc-in [:cfg :consumption-types]
                                (:consumption-types msg-payload))
                      (assoc-in [:cfg :mentions] (:mentions msg-payload)))]
    {:new-state new-state}))

(defn initial-state-fn
  "Creates the initial component state atom. Holds a list of entries from the
   backend, a map with temporary entries that are being edited but not saved
   yet, and sets that contain information for which entries to show the map,
   or the edit mode."
  [put-fn]
  (let [initial-state (atom {:entries        []
                             :last-alive     (st/now)
                             :new-entries    @cse/new-entries-ls
                             :current-query  @s/queries
                             :pomodoro-stats (sorted-map)
                             :activity-stats (sorted-map)
                             :task-stats     (sorted-map)
                             :cfg            {:active             nil
                                              :linked-filter      {}
                                              :show-maps-for      #{}
                                              :show-comments-for  #{}
                                              :sort-by-upvotes    false
                                              :show-all-maps      false
                                              :show-hashtags      true
                                              :comments-w-entries true
                                              :show-context       true
                                              :mute               false
                                              :show-pvt           false
                                              :lines-shortened    3}})]
    (put-fn [:state/stats-tags-get])
    (doseq [[_id q] (:current-query @initial-state)]
      (put-fn [:state/get q]))
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

(defn set-conj-fn
  "Like toggle-set-fn but only adds timestamp to set specified in path.
   Noop if already in there."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)
        ts (:timestamp msg-payload)
        new-state (update-in current-state path conj ts)]
    {:new-state new-state}))

(defn toggle-key-fn
  "Toggles config key."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)]
    {:new-state (update-in current-state path not)}))

(defn show-more-fn
  "Runs previous query but with more results. Also updates the number to show in
   the UI."
  [{:keys [current-state msg-payload]}]
  (let [query-id (:query-id msg-payload)
        current-query (merge (query-id (:current-query current-state))
                             msg-payload)
        new-query (update-in current-query [:n] + 20)
        new-state (assoc-in current-state [:current-query query-id] new-query)]
    {:new-state new-state
     :emit-msg  [:state/get new-query]}))

(defn toggle-active-fn
  "Sets entry in payload as the active entry for which to show linked entries."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [timestamp query-id]} msg-payload
        currently-active (get-in current-state [:cfg :active query-id])]
    {:new-state (assoc-in current-state [:cfg :active query-id]
                          (if (= currently-active timestamp)
                            nil
                            timestamp))
     :emit-msg  s/update-location-hash-msg}))

(defn toggle-lines
  "Toggle number of lines to show when comments are shortend. Cycles from
   one to ten."
  [{:keys [current-state]}]
  {:new-state (update-in current-state [:cfg :lines-shortened]
                         #(if (< % 10) (inc %) 1))})

(defn save-stats
  "Stores received stats on component state."
  [k]
  (fn [{:keys [current-state msg-payload]}]
    (let [ds (:date-string msg-payload)
          new-state (assoc-in current-state [k ds] msg-payload)]
      {:new-state new-state})))

(defn set-currently-dragged
  "Set the currently dragged entry for drag and drop."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (assoc-in current-state [:cfg :currently-dragged] ts)]
    {:new-state new-state}))

(defn cmp-map
  "Creates map for the component which holds the client-side application state."
  [cmp-id]
  {:cmp-id            cmp-id
   :state-fn          initial-state-fn
   :snapshot-xform-fn #(dissoc % :last-alive)
   :state-spec        :state/client-store-spec
   :handler-map       (merge cse/entry-handler-map
                             s/search-handler-map
                             {:state/new          new-state-fn
                              :stats/pomo-day     (save-stats :pomodoro-stats)
                              :stats/activity-day (save-stats :activity-stats)
                              :stats/tasks-day    (save-stats :task-stats)
                              :state/stats-tags   stats-tags-fn
                              :show/more          show-more-fn
                              :cmd/toggle-active  toggle-active-fn
                              :cmd/toggle         toggle-set-fn
                              :cmd/set-opt        set-conj-fn
                              :cmd/set-dragged    set-currently-dragged
                              :cmd/toggle-key     toggle-key-fn
                              :cmd/keep-alive     ka/reset-fn
                              :cmd/keep-alive-res ka/set-alive-fn
                              :cmd/toggle-lines   toggle-lines})})

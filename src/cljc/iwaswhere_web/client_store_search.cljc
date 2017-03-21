(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :as sa])
    [iwaswhere-web.client-store-cfg :as c]
    [matthiasn.systems-toolbox.component :as st]
    [iwaswhere-web.utils.parse :as p]
    [clojure.pprint :as pp]
    [clojure.set :as set]))

(def initial-query-cfg
  {:queries    {}
   :tab-groups {:left  {:active nil :all #{}}
                :right {:active nil :all #{}}}})

#?(:clj  (defonce query-cfg (atom initial-query-cfg))
   :cljs (defonce query-cfg (sa/local-storage
                              (atom initial-query-cfg) "iWasWhere_query_cfg")))

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked
   entries view."
  [{:keys [current-state msg-payload]}]
  (let [query-id (:query-id msg-payload)
        query-path [:query-cfg :queries query-id]
        query-msg (merge msg-payload
                         {:sort-asc (:sort-asc (:cfg current-state))})
        new-state (assoc-in current-state query-path query-msg)]
    (swap! query-cfg assoc-in [:queries query-id] msg-payload)
    {:new-state new-state
     :emit-msg  [:state/search (:query-cfg new-state)]}))

; TODO: linked filter belongs in query-cfg
(defn set-linked-filter
  "Sets search in linked entries column."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [search query-id]} msg-payload]
    {:new-state    (assoc-in current-state [:cfg :linked-filter query-id] search)
     :send-to-self [:cfg/save]}))

(defn set-active-query
  "Sets active query for specified tab group."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [query-id tab-group]} msg-payload
        path [:query-cfg :tab-groups tab-group :active]
        new-state (-> current-state
                      (assoc-in path query-id)
                      (update-in [:query-cfg :tab-groups tab-group :history]
                                 #(conj (take 50 %1) %2)
                                 query-id))]
    (when (-> current-state :query-cfg :queries query-id)
      (reset! query-cfg (:query-cfg new-state))
      {:new-state new-state})))

(defn find-existing
  "Finds existing query in the same tab group with the same search-text."
  [query-cfg tab-group query]
  (let [all-path [:tab-groups tab-group :all]
        queries (map #(get-in query-cfg [:queries %]) (get-in query-cfg all-path))
        matching (filter #(and (= (:search-text query) (:search-text %))
                               (= (:story query) (:story %)))
                         queries)]
    (first matching)))

(defn add-query
  "Adds query inside tab group specified in msg if none exists already with the
   same search-text. Otherwise opens the existing one."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [tab-group query]} msg-payload
        query-id (keyword (st/make-uuid))
        active-path [:query-cfg :tab-groups tab-group :active]
        all-path [:query-cfg :tab-groups tab-group :all]]
    (if-let [existing (find-existing (:query-cfg current-state) tab-group query)]
      (let [query-id (:query-id existing)
            new-state (assoc-in current-state active-path query-id)]
        (reset! query-cfg (:query-cfg new-state))
        {:new-state new-state})
      (let [new-query (merge {:query-id query-id} (p/parse-search "") query)
            new-state (-> current-state
                          (assoc-in active-path query-id)
                          (assoc-in [:query-cfg :queries query-id] new-query)
                          (update-in all-path conj query-id)
                          (update-in [:query-cfg :tab-groups tab-group :history]
                                     #(conj (take 20 %1) %2)
                                     query-id))]
        (reset! query-cfg (:query-cfg new-state))
        {:new-state new-state}))))

(defn previously-active
  "Sets active query for the tab group to the previously active query."
  [state query-id tab-group]
  (let [all-path [:query-cfg :tab-groups tab-group :all]
        active-path [:query-cfg :tab-groups tab-group :active]
        hist-path [:query-cfg :tab-groups tab-group :history]]
    (update-in state active-path
               (fn [active]
                 (if (= active query-id)
                   (let [hist (get-in state hist-path)]
                     (first (filter
                              #(contains? (get-in state all-path) %)
                              hist)))
                   active)))))

(defn remove-query
  "Remove query inside tab group specified in msg."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [query-id tab-group]} msg-payload
        all-path [:query-cfg :tab-groups tab-group :all]
        query-path [:query-cfg :queries]
        new-state (-> current-state
                      (update-in all-path disj query-id)
                      (previously-active query-id tab-group)
                      (update-in query-path dissoc query-id)
                      (update-in [:results] dissoc query-id))]
    (reset! query-cfg (:query-cfg new-state))
    {:new-state new-state}))

(defn set-dragged-fn
  "Set actively dragged tab so it's available when dropped onto another element."
  [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:query-cfg :dragged] msg-payload)]
    (reset! query-cfg (:query-cfg new-state))
    {:new-state new-state}))

(defn move-tab-fn
  "Moves query tab from one tab-group to another."
  [{:keys [current-state msg-payload]}]
  (let [dragged (:dragged msg-payload)
        q-id (:query-id dragged)
        from (:tab-group dragged)
        to (:to msg-payload)
        new-state (-> current-state
                      (assoc-in [:query-cfg :tab-groups to :active] q-id)
                      (update-in [:query-cfg :tab-groups to :all] conj q-id)
                      (update-in [:query-cfg :tab-groups from :all] disj q-id)
                      (previously-active q-id from))]
    (reset! query-cfg (:query-cfg new-state))
    {:new-state new-state}))

(defn show-more-fn
  "Runs previous query but with more results. Also updates the number to show in
   the UI."
  [{:keys [current-state msg-payload]}]
  (let [query-path [:query-cfg :queries (:query-id msg-payload)]
        merged (merge (get-in current-state query-path) msg-payload)
        new-query (update-in merged [:n] + 20)]
    {:send-to-self [:search/update new-query]}))

(defn search-refresh-fn
  "Refreshes client-side state by sending all queries, plus, with a delay,
   the stats and tags."
  [{:keys [current-state]}]
  (let [query-cfg (:query-cfg current-state)]
    {:emit-msg [[:state/search query-cfg]
                [:cmd/schedule-new {:timeout 200
                                    :message [:state/stats-tags-get]}]]}))

(def search-handler-map
  {:search/update      update-query-fn
   :search/set-active  set-active-query
   :search/add         add-query
   :search/remove      remove-query
   :search/refresh     search-refresh-fn
   :search/set-dragged set-dragged-fn
   :search/move-tab    move-tab-fn
   :show/more          show-more-fn
   :linked-filter/set  set-linked-filter})

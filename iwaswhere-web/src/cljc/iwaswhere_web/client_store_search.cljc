(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :as sa])
    [iwaswhere-web.client-store-cfg :as c]
    [matthiasn.systems-toolbox.component :as st]
    [iwaswhere-web.utils.parse :as p]
    [clojure.pprint :as pp]))

(def initial-query-cfg
  {:queries    {}
   :tab-groups {:left  {:active nil :all #{}}
                :right {:active nil :all #{}}}})

#?(:clj  (defonce query-cfg (atom initial-query-cfg))
   :cljs (defonce query-cfg (sa/local-storage
                              (atom initial-query-cfg) "iWasWhere_query_cfg")))

(def update-location-hash-msg
  [:cmd/schedule-new {:timeout 5000 :message [:search/set-hash]}])

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked
   entries view."
  [{:keys [current-state msg-payload]}]
  (let [query-id (:query-id msg-payload)
        query-path [:query-cfg :queries query-id]
        new-state (assoc-in current-state query-path msg-payload)
        query-msg (merge msg-payload
                         {:sort-by-upvotes (:sort-by-upvotes current-state)
                          :sort-asc (:sort-asc (:cfg current-state))})]
    (swap! query-cfg assoc-in [:queries query-id] msg-payload)
    {:new-state new-state
     :emit-msg  [[:state/get query-msg]
                 update-location-hash-msg]}))

; TODO: linked filter belongs in query-cfg
(defn set-linked-filter
  "Sets search in linked entries column."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [search query-id]} msg-payload]
    {:new-state (assoc-in current-state [:cfg :linked-filter query-id] search)
     :emit-msg  update-location-hash-msg}))

(defn set-active-query
  "Sets active query for specified tab group."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [query-id tab-group]} msg-payload
        path [:query-cfg :tab-groups tab-group :active]
        new-state (assoc-in current-state path query-id)]
    (when (-> current-state :query-cfg :queries query-id)
      (reset! query-cfg (:query-cfg new-state))
      {:new-state new-state})))

(defn add-query
  "Adds query inside tab group specified in msg."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [tab-group]} msg-payload
        query-id (keyword (st/make-uuid))
        active-path [:query-cfg :tab-groups tab-group :active]
        all-path [:query-cfg :tab-groups tab-group :all]
        new-state (-> current-state
                      (assoc-in active-path query-id)
                      (update-in all-path conj query-id))]
    (reset! query-cfg (:query-cfg new-state))
    {:new-state new-state
     :send-to-self [:search/update (merge {:query-id query-id}
                                          (p/parse-search ""))]}))

(defn remove-query
  "Remove query inside tab group specified in msg."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [query-id tab-group]} msg-payload
        all-path [:query-cfg :tab-groups tab-group :all]
        active-path [:query-cfg :tab-groups tab-group :active]
        query-path [:query-cfg :queries]
        new-state (-> current-state
                      (update-in all-path disj query-id)
                      (update-in active-path #(if (= % query-id) nil %))
                      (update-in query-path dissoc query-id)
                      (update-in [:results] dissoc query-id))]
    (reset! query-cfg (:query-cfg new-state))
    {:new-state new-state}))

(def search-handler-map
  {:search/update     update-query-fn
   :search/set-active set-active-query
   :search/add        add-query
   :search/remove     remove-query
   :linked-filter/set set-linked-filter})

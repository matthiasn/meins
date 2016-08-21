(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    [matthiasn.systems-toolbox.component :as st]))

(def update-location-hash-msg [:cmd/schedule-new {:timeout 5000 :message [:search/set-hash]}])

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked
   entries view."
  [{:keys [current-state msg-payload]}]
  (let [query-id (:query-id msg-payload)
        new-state (assoc-in current-state [:current-query query-id] msg-payload)
        sort-by-upvotes? (:sort-by-upvotes current-state)
        query-msg (merge msg-payload {:sort-by-upvotes sort-by-upvotes?})]
    {:new-state new-state
     :emit-msg  [[:state/get query-msg]
                 update-location-hash-msg]}))

(defn set-linked-filter
  "Sets search in linked entries column."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [search query-id]} msg-payload]
    {:new-state (assoc-in current-state [:cfg :linked-filter query-id] search)
     :emit-msg  update-location-hash-msg}))

(def search-handler-map
  {:search/update     update-query-fn
   :linked-filter/set set-linked-filter})

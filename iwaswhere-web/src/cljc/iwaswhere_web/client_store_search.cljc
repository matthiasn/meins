(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :as sa])
    [matthiasn.systems-toolbox.component :as st]))

(def initial-query-cfg
  {:queries    {}
   :tab-groups {:left  {:active :query-1
                        :all    #{:query-1}}
                :right {:active :query-2
                        :all    #{:query-2}}}})

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
        query-path  [:query-cfg :queries query-id]
        new-state (assoc-in current-state query-path msg-payload)
        sort-by-upvotes? (:sort-by-upvotes current-state)
        query-msg (merge msg-payload {:sort-by-upvotes sort-by-upvotes?})]
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

(def search-handler-map
  {:search/update     update-query-fn
   :linked-filter/set set-linked-filter})

(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    [matthiasn.systems-toolbox.component :as st]))

(def update-location-hash-msg [:cmd/schedule-new {:timeout 5000 :message [:search/set-hash]}])

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked entries view."
  [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:current-query] msg-payload)]
    {:new-state new-state
     :emit-msg  [[:state/get (merge msg-payload {:sort-by-upvotes (:sort-by-upvotes current-state)})]
                 update-location-hash-msg]}))

(defn set-location-hash
  "Set browser's location hash."
  [search-text]
  #?(:cljs (aset js/window "location" "hash" (js/encodeURIComponent search-text))))

(defn set-location-handler
  "Set location hash for combination of search, active entry, and linked-filter."
  [{:keys [current-state]}]
  (let [search-text (:search-text (:current-query current-state))
        active-entry (-> current-state :cfg :active)
        linked-filter (-> current-state :cfg :linked-filter :search-text)]
    (set-location-hash (str search-text "|" active-entry "|" linked-filter))))

(defn set-linked-filter
  "Sets search in linked entries column."
  [{:keys [current-state msg-payload]}]
  {:new-state (assoc-in current-state [:cfg :linked-filter] msg-payload)
   :emit-msg update-location-hash-msg})

(def search-handler-map
  {:search/update     update-query-fn
   :search/set-hash   set-location-handler
   :linked-filter/set set-linked-filter})

(ns iwaswhere-web.client-store-search
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    [matthiasn.systems-toolbox.component :as st]))

(defn update-query-fn
  "Update query in client state, with resetting the active entry in the linked entries view."
  [{:keys [current-state msg-payload]}]
  (let [new-state (-> current-state
                      (assoc-in [:current-query] msg-payload)
                      (assoc-in [:active] nil))]
    {:new-state new-state
     :emit-msg  [[:state/get (merge msg-payload {:sort-by-upvotes (:sort-by-upvotes current-state)})]
                 [:cmd/schedule-new {:timeout 5000 :message [:search/set-hash]}]]}))

(defn set-location-hash
  "Set browser's location hash."
  [search-text]
  #?(:cljs (aset js/window "location" "hash" (js/encodeURIComponent search-text))))

(defn set-location-handler
  "Update query in client state, with resetting the active entry in the linked entries view."
  [{:keys [current-state]}]
  (set-location-hash (str (:search-text (:current-query current-state))
                          "|"
                          (-> current-state :cfg :active))))

(def search-handler-map
  {:search/update   update-query-fn
   :search/set-hash set-location-handler})

(ns iwaswhere-web.store
  (:require [clojure.pprint :as pp]))

(defn geo-entry-persist-fn
  "Handler function for persisting new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [new-state (update-in current-state [:entries] conj msg-payload)
        filename (str "./data/" (:timestamp msg-payload) ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg [:state/new new-state]}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [_put-fn] {:state (atom {:entries []})})
   :handler-map {:geo-entry/persist geo-entry-persist-fn}})

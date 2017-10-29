(ns i-was-where-app.ios.store
  (:require [matthiasn.systems-toolbox.component :as st]
            [glittershark.core-async-storage :as as]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def device-info (js/require "react-native-device-info"))

(defn persist [{:keys [current-state put-fn msg-payload]}]
  (let [{:keys [timestamp vclock id]} msg-payload
        last-vclock (:latest-vclock current-state)
        device-id (.getUniqueID device-info)
        new-vclock (update-in last-vclock [device-id] #(inc (or % 0)))
        new-vclock (merge vclock new-vclock)
        id (or id (st/make-uuid))
        entry (merge msg-payload
                     {:last-saved (st/now)
                      :id         id
                      :vclock     new-vclock})
        new-state (-> current-state
                      (assoc-in [:entries timestamp] entry)
                      (assoc-in [:latest-vclock] new-vclock))]
    (go
      (<! (as/set-item timestamp entry))
      (println :persisted (<! (as/get-item timestamp))))
    {:new-state new-state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:entry/update persist
                 :entry/new    persist}})

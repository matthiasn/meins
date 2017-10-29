(ns i-was-where-app.ios.store
  (:require [matthiasn.systems-toolbox.component :as st]))

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
    (prn msg-payload)
    {:new-state new-state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:entry/update persist
                 :entry/new    persist}})

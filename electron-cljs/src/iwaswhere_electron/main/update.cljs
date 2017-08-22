(ns iwaswhere-electron.main.update
  (:require [iwaswhere-electron.main.log :as log]
            [electron-updater :refer [autoUpdater]]))

(defn state-fn
  [put-fn]
  (let []
    (log/info "Starting UPDATE Component")
    (.on autoUpdater "checking-for-update" (fn [] (log/info "Checking for update...")))
    {:state (atom {})}))

(defn check-updates
  [{:keys []}]
  (log/info "UPDATE: check")
  (.checkForUpdates autoUpdater)
  {})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:update/check check-updates}})


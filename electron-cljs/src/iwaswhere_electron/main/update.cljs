(ns iwaswhere-electron.main.update
  (:require [iwaswhere-electron.main.log :as log]
            [electron-updater :refer [autoUpdater]]))

(defn state-fn
  [put-fn]
  (let []
    (log/info "Starting UPDATE Component")
    (.on autoUpdater "checking-for-update" (fn [] (log/info "Checking for update...")))
    (.on autoUpdater "update-available" (fn [] (log/info "Update available.")))
    (.on autoUpdater "update-not-available" (fn [] (log/info "Update not available.")))
    (.on autoUpdater "error" (fn [] (log/info "Error in auto-updater.")))
    {:state (atom {})}))

(defn check-updates
  [{:keys []}]
  (log/info "UPDATE: check")
  (.checkForUpdates autoUpdater)
  {})

(defn install-updates
  [{:keys []}]
  (log/info "UPDATE: install")
  (.quitAndInstall autoUpdater false)
  {})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:update/check check-updates
                 :update/install install-updates}})


(ns iwaswhere-electron.main.update
  (:require [iwaswhere-electron.main.log :as log]
            [electron-updater :refer [autoUpdater]]))

(defn state-fn
  [put-fn]
  (let [no-update-available (fn [_]
                              (log/info "Update not available.")
                              (put-fn [:update/status {:status :update/not-available}]))
        update-available (fn [_]
                           (log/info "Update available.")
                           (put-fn [:update/status {:status :update/available}]))
        checking (fn [_]
                   (log/info "Checking for update...")
                   (put-fn [:update/status {:status :update/checking}]))
        downloaded (fn [ev]
                     (log/info "Update downloaded")
                     (put-fn [:update/status {:status :update/downloaded}]))
        downloading (fn [progress]
                      (let [info (js->clj progress :keywordize-keys true)]
                        (log/info "Update downloading" (str info))
                        (put-fn [:update/status {:status :update/downloading
                                                 :info   info}])))
        error (fn [ev]
                (log/info "Error in auto-updater" ev)
                (put-fn [:update/status :update/error]))]
    (log/info "Starting UPDATE Component")
    (.on autoUpdater "checking-for-update" checking)
    (.on autoUpdater "update-available" update-available)
    (.on autoUpdater "update-not-available" no-update-available)
    (.on autoUpdater "update-downloaded" downloaded)
    (.on autoUpdater "download-progress" downloading)
    (.on autoUpdater "error" error)
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
   :handler-map {:update/check   check-updates
                 :update/install install-updates}})


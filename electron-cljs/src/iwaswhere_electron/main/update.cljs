(ns iwaswhere-electron.main.update
  (:require [iwaswhere-electron.main.log :as log]
            [electron-log :as electron-log]
            [electron-updater :refer [autoUpdater]]))

(defn state-fn
  [put-fn]
  (let [state (atom {:open-window false})
        no-update-available (fn [_]
                              (log/info "Update not available.")
                              (put-fn [:update/status {:status :update/not-available}]))
        update-available (fn [info]
                           (let [info (js->clj info :keywordize-keys true)]
                             (log/info "Update available.")
                             (if (:open-window @state)
                               (put-fn [:window/updater])
                               (put-fn [:update/status {:status :update/available
                                                        :info   info}]))))
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
                (put-fn [:update/status {:status :update/error}]))]
    (log/info "Starting UPDATE Component")
    (aset autoUpdater "autoDownload" false)
    (aset autoUpdater "logger" electron-log)
    (.on autoUpdater "checking-for-update" checking)
    (.on autoUpdater "update-available" update-available)
    (.on autoUpdater "update-not-available" no-update-available)
    (.on autoUpdater "update-downloaded" downloaded)
    (.on autoUpdater "download-progress" downloading)
    (.on autoUpdater "error" error)
    {:state state}))

(defn check-updates
  [open-window]
  (fn [{:keys [current-state]}]
    (log/info "UPDATE: check")
    (.checkForUpdates autoUpdater)
    {:new-state (assoc-in current-state [:open-window] open-window)}))

(defn download-updates
  [{:keys []}]
  (log/info "UPDATE: download")
  (.downloadUpdate autoUpdater)
  {})

(defn install-updates
  [{:keys []}]
  (log/info "UPDATE: install")
  {:emit-msg [[:app/clear-cache]
              ;[:app/clear-iww-cache]
              [:app/shutdown-jvm]
              [:cmd/schedule-new {:timeout 1000
                                  :message [:update/quit-install]}]]})

(defn quit-install
  [{:keys []}]
  (log/info "UPDATE: quit and install")
  (.quitAndInstall autoUpdater false)
  {})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:update/check        (check-updates false)
                 :update/auto-check   (check-updates true)
                 :update/download     download-updates
                 :update/install      install-updates
                 :update/quit-install quit-install}})


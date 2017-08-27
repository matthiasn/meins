(ns iwaswhere-electron.main.update-window
  (:require [clojure.string :as str]
            [iwaswhere-electron.main.log :as log]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]
            [iwaswhere-electron.main.runtime :as rt]))

(defn updater-window
  [{:keys [current-state cmp-state put-fn]}]
  (when-let [existing (:updater-window current-state)]
    (.close existing))
  (let [window (BrowserWindow. (clj->js {:width 600 :height 300}))
        url (str "file://" (:app-path rt/runtime-info) "/updater.html")
        new-state (assoc-in current-state [:updater-window] window)
        close (fn [_]
                (log/info "Closed updater-window")
                (swap! cmp-state assoc-in [:updater-window] nil))]
    (log/info "Opening new updater window" url)
    (.loadURL window url)
    (.on window "focus" #(swap! cmp-state assoc-in [:active] true))
    (.on window "blur" #(swap! cmp-state assoc-in [:active] false))
    (.on window "close" close)
    {:new-state new-state}))

(defn relay-msg
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (when-let [updater-window (:updater-window current-state)]
    (let [web-contents (.-webContents updater-window)
          serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
      (log/info "Relaying" (str msg-type) (str msg-payload))
      (.send web-contents "relay" (pr-str serializable))))
  {})

(defn close-window
  [{:keys [current-state]}]
  (when-let [updater-window (:updater-window current-state)]
    (when (:active current-state)
      (log/info "Closing Updater Window:")
      (.close updater-window)))
  {})

(defn state-fn
  [put-fn]
  (let [state (atom {})
        relay (fn [ev m]
                (let [parsed (read-string m)
                      msg-type (first parsed)
                      {:keys [msg-payload msg-meta]} (second parsed)
                      msg (with-meta [msg-type msg-payload] msg-meta)]
                  (log/info "Update IPC relay:" (with-out-str (pp/pprint msg)))
                  (if (= msg-type :window/close)
                    (close-window {:current-state @state})
                    (put-fn msg))))]
    (.on ipcMain "relay" relay)
    {:state state}))

(defn cmp-map
  [cmp-id]
  (let [relay-types #{:update/status}
        relay-map (zipmap relay-types (repeat relay-msg))]
    {:cmp-id      cmp-id
     :state-fn    state-fn
     :handler-map (merge relay-map
                         {:window/updater updater-window
                          :window/close   close-window})}))

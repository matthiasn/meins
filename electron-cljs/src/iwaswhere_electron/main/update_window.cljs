(ns iwaswhere-electron.main.update-window
  (:require [clojure.string :as str]
            [iwaswhere-electron.main.log :as log]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]))

(defn updater-window
  [{:keys [current-state cmp-state put-fn]}]
  (let [window (BrowserWindow. (clj->js {:width 600 :height 300}))
        window-id (stc/make-uuid)
        cwd (.cwd process)
        rp (.-resourcesPath process)
        url (if (= "/" cwd)
              (str "file://" rp "/app/updater.html")
              (str "file://" cwd "/updater.html"))
        new-state (-> current-state
                      (assoc-in [:windows window-id] window)
                      (assoc-in [:active] window-id))
        focus (fn [_]
                (log/info "Focused updater-window" window-id)
                (swap! cmp-state assoc-in [:active] window-id))
        blur (fn [_]
               (log/info "Blurred updater-window" window-id)
               (swap! cmp-state assoc-in [:active] nil))
        close (fn [_]
                (log/info "Closed updater-window")
                (swap! cmp-state assoc-in [:updater-window] nil))]
    (log/info "Opening new updater window" url cwd)
    (.loadURL window url)
    (.on window "focus" focus)
    (.on window "blur" blur)
    (.on window "close" close)
    {:new-state new-state}))

(defn active-window
  [current-state]
  (let [active (:active current-state)]
    (get-in current-state [:windows active])))

(defn web-contents
  [current-state]
  (when-let [active-window (active-window current-state)]
    (.-webContents active-window)))

(defn relay-msg
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (when-let [web-contents (web-contents current-state)]
    (let [serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
      (log/info "Relaying" (str msg-type) (str msg-payload))
      (.send web-contents "relay" (pr-str serializable))))
  {})

(defn dev-tools
  [{:keys [current-state]}]
  (when-let [web-contents (web-contents current-state)]
    (.openDevTools web-contents))
  {})

(defn close-window
  [{:keys [current-state]}]
  (when-let [active-window (active-window current-state)]
    (log/info "Closing Updater Window:" (:active current-state))
    (.close active-window))
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
                         {:window/updater   updater-window
                          :window/close     close-window
                          :window/dev-tools dev-tools})}))

(ns iwaswhere-electron.main.window-manager
  (:require [clojure.string :as str]
            [iwaswhere-electron.main.log :as log]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]))

(defn new-window
  [{:keys [current-state cmp-state]}]
  (let [window (BrowserWindow. (clj->js {:width 1200 :height 800}))
        window-id (stc/make-uuid)
        cwd (.cwd process)
        rp (.-resourcesPath process)
        url (if (= "/" cwd)
              (str "file://" rp "/app/index.html")
              (str "file://" cwd "/index.html"))
        new-state (-> current-state
                      (assoc-in [:main-window] window)
                      (assoc-in [:windows window-id] window)
                      (assoc-in [:active] window-id))
        focus (fn [_]
                (log/info "Focused" window-id)
                (swap! cmp-state assoc-in [:active] window-id))
        close (fn [_]
                (log/info "Closed" window-id)
                (swap! cmp-state assoc-in [:active] nil)
                (swap! cmp-state update-in [:windows] dissoc window-id))]
    (log/info "Opening new window" url cwd)
    (.loadURL window url)
    (.on window "focus" focus)
    (.on window "close" close)
    {:new-state new-state}))

(defn updater-window
  [{:keys [current-state cmp-state put-fn]}]
  (let [window (BrowserWindow. (clj->js {:width 1200 :height 800}))
        cwd (.cwd process)
        rp (.-resourcesPath process)
        url (if (= "/" cwd)
              (str "file://" rp "/app/updater.html")
              (str "file://" cwd "/updater.html"))
        new-state (assoc-in current-state [:updater-window] window)
        focus (fn [_]
                (log/info "Focused updater-window"))
        close (fn [_]
                (log/info "Closed updater-window")
                (swap! cmp-state assoc-in [:updater-window] nil))]
    (log/info "Opening new updater window" url cwd)
    (.loadURL window url)
    (.openDevTools (.-webContents window))
    (.on window "focus" focus)
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

(defn send-cmd
  [{:keys [current-state msg-payload]}]
  (let [{:keys [cmd-type cmd]} msg-payload]
    (when-let [web-contents (web-contents current-state)]
      (.send web-contents cmd-type cmd))
    {}))

(defn relay-msg
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (when-let [web-contents (web-contents current-state)]
    (let [serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
      (log/info "Relaying" (str msg-type) (str msg-payload))
      (.send web-contents "relay" (pr-str serializable))))
  {})

(defn update-check
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [updater-window (:updater-window current-state)]
    (when-let [web-contents (.-webContents updater-window)]
      (let [serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
        (log/info "Relaying update-check" (str msg-type) (str msg-payload))
        (.send web-contents "relay" (pr-str serializable)))))
  {})

(defn dev-tools
  [{:keys [current-state]}]
  (when-let [web-contents (web-contents current-state)]
    (.openDevTools web-contents))
  {})

(defn close-window
  [{:keys [current-state]}]
  (when-let [active-window (active-window current-state)]
    (.close active-window))
  {})

(defn activate
  [{:keys [current-state]}]
  (log/info "Activate APP")
  (when (empty? (:windows current-state))
    {:send-to-self [:window/new]}))

(defn state-fn
  [put-fn]
  (let [relay-handler (fn [ev m]
                        (let [parsed (read-string m)
                              msg-type (first parsed)
                              {:keys [msg-payload msg-meta]} (second parsed)
                              msg (with-meta [msg-type msg-payload] msg-meta)]
                          (log/info "IPC relay:" (with-out-str (pp/pprint msg)))
                          (put-fn msg)))]
    (.on ipcMain "relay" relay-handler)
    {:state (atom {})}))

(defn cmp-map
  [cmp-id relay-types]
  (let [relay-map (zipmap relay-types (repeat relay-msg))]
    {:cmp-id      cmp-id
     :state-fn    state-fn
     :handler-map (merge relay-map
                         {:window/new       new-window
                          :window/updater   updater-window
                          :update/check     update-check
                          :window/activate  activate
                          :window/send      send-cmd
                          :window/close     close-window
                          :window/dev-tools dev-tools})}))

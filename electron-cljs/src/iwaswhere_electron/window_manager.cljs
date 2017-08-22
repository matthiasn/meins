(ns iwaswhere-electron.window-manager
  (:require [clojure.string :as str]
            [iwaswhere-electron.log :as log]
            [electron :refer [BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [clojure.pprint :as pp]))

(defn new-window
  [{:keys [current-state cmp-state]}]
  (let [window (BrowserWindow. {:width 1200 :height 800})
        window-id (stc/make-uuid)
        cwd (.cwd process)
        url (str "file://" cwd "/index.html")
        new-state (-> current-state
                      (assoc-in [:main-window] window)
                      (assoc-in [:windows window-id] window)
                      (assoc-in [:active] window-id))
        focus (fn [_]
                (log/info "Focused" window-id)
                (swap! cmp-state assoc-in [:active] window-id))
        close (fn [_]
                (log/info "Closed" window-id)
                (swap! cmp-state assoc-in [:active] nil))]
    (log/info "Opening new window" url)
    (.loadURL window url)
    (pp/pprint new-state)
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


(defn dev-tools
  [{:keys [current-state msg-payload]}]
  (when-let [web-contents (web-contents current-state)]
    (.openDevTools web-contents))
  {})


(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:window/new       new-window
                 :window/send      send-cmd
                 :window/dev-tools dev-tools}})

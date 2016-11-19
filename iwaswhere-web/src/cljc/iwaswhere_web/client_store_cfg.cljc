(ns iwaswhere-web.client-store-cfg
  (:require #?(:cljs [alandipert.storage-atom :as sa])
    [matthiasn.systems-toolbox.component :as st]))

(def default-config
  {:active            nil
   :linked-filter     {}
   :show-context      true
   :show-maps-for     #{}
   :show-comments-for {}
   :show-pvt          true
   :thumbnails        true
   :reconfigure-grid  true
   :lines-shortened   3})

#?(:clj  (defonce app-cfg (atom default-config))
   :cljs (defonce app-cfg (sa/local-storage (atom default-config)
                                            "iWasWhere_cfg")))

(defn save-cfg
  "Saves current configuration in localstorage."
  [{:keys [current-state]}]
  (reset! app-cfg (:cfg current-state)))

(defn toggle-key-fn
  "Toggles config key. If reset key is set, changes the value in path to the
   specified value, rather than applying the 'not' function."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [path reset-to]} msg-payload
        new-state (if reset-to
                    (assoc-in current-state path reset-to)
                    (update-in current-state path not))]
    {:new-state    new-state
     :send-to-self [:cfg/save]}))

(defn toggle-lines
  "Toggle number of lines to show when comments are shortend. Cycles from
   one to ten."
  [{:keys [current-state]}]
  {:new-state    (update-in current-state [:cfg :lines-shortened]
                            #(if (< % 10) (inc %) 1))
   :send-to-self [:cfg/save]})

(defn set-currently-dragged
  "Set the currently dragged entry for drag and drop."
  [{:keys [current-state msg-payload]}]
  (let [ts (:timestamp msg-payload)
        new-state (assoc-in current-state [:cfg :currently-dragged] ts)]
    {:new-state new-state}))

(defn toggle-active-fn
  "Sets entry in payload as the active entry for which to show linked entries."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [timestamp query-id]} msg-payload
        currently-active (get-in current-state [:cfg :active query-id])
        new-state (assoc-in current-state [:cfg :active query-id]
                            (when-not (= currently-active timestamp)
                              timestamp))]
    {:new-state new-state
     :send-to-self [:cfg/save]}))

(defn toggle-set-fn
  "Toggles for example the visibility of a map or the edit mode for an individual
  journal entry. Requires the key to exist on the application state as a set."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)
        timestamp (:timestamp msg-payload)
        new-state (if (contains? (get-in current-state path) timestamp)
                    (update-in current-state path disj timestamp)
                    (update-in current-state path conj timestamp))]
    {:new-state    new-state
     :send-to-self [:cfg/save]}))

(defn set-conj-fn
  "Like toggle-set-fn but only adds timestamp to set specified in path.
   Noop if already in there."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)
        ts (:timestamp msg-payload)
        new-state (update-in current-state path conj ts)]
    {:new-state    new-state
     :send-to-self [:cfg/save]}))

(defn assoc-in-state
  "Assoc the provided value in the app state at the provided path."
  [{:keys [current-state msg-payload]}]
  (let [path (:path msg-payload)
        value (:value msg-payload)
        new-state (assoc-in current-state path value)]
    {:new-state    new-state
     :send-to-self [:cfg/save]}))

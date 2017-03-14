(ns iwaswhere-web.client-store-cfg
  (:require #?(:cljs [alandipert.storage-atom :as sa])
    [matthiasn.systems-toolbox.component :as st]
    [clojure.pprint :as pp]))

(def default-config
  {:active            nil
   :linked-filter     {}
   :show-context      true
   :show-maps-for     #{}
   :show-comments-for {}
   :show-pvt          true
   :thumbnails        true
   :reconfigure-grid  true
   :lines-shortened   10
   :widgets           {:tabs-left     {:type      :tabs-view
                                       :query-id  :left
                                       :data-grid {:x 6 :y 0 :w 9 :h 19}}
                       :tabs-right    {:type      :tabs-view
                                       :query-id  :right
                                       :data-grid {:x 15 :y 0 :w 9 :h 19}}
                       :calendar      {:type      :calendar
                                       :data-grid {:x 0 :y 0 :w 6 :h 10}}
                       :custom-fields {:type      :custom-fields-chart
                                       :data-grid {:x 0 :y 3 :w 6 :h 10}}
                       :all-stats     {:type      :all-stats-chart
                                       :data-grid {:x 0 :y 6 :w 6 :h 9}}}})

#?(:clj  (defonce app-cfg (atom default-config))
   :cljs (defonce app-cfg (sa/local-storage (atom default-config)
                                            "iWasWhere_cfg")))

(defn save-layout
  "Saves current layout in config."
  [{:keys [current-state msg-payload]}]
  (let [mapper (fn [widget]
                 (let [k (keyword (:i widget))
                       data-grid (select-keys widget [:x :y :w :h])]
                   [k {:data-grid data-grid}]))
        new-layout (into {} (map mapper msg-payload))
        merged (merge-with merge (:widgets (:cfg current-state)) new-layout)
        new-state (assoc-in current-state [:cfg :widgets] merged)]
    {:new-state    new-state
     :send-to-self [:cfg/save]}))

(defn save-cfg
  "Saves current configuration in localstorage."
  [{:keys [current-state]}]
  (reset! app-cfg (:cfg current-state))
  {})

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
    {:new-state    new-state
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

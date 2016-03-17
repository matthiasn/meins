(ns iwaswhere-web.store
  (:require [clojure.pprint :as pp]))

(defn geo-entry-persist-fn
  "Handler function for persisting new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [new-state (update-in current-state [:entries] conj msg-payload)
        filename (str "./data/" (:timestamp msg-payload) ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg  [:state/new new-state]}))

(defn state-get-fn
  "Handler function for retrieving current state."
  [{:keys [current-state]}]
  {:emit-msg [:state/new current-state]})

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state."
  [_put-fn]
  (let [state (atom {:entries []})
        files (file-seq (clojure.java.io/file "./data"))]
    (doseq [f (filter-by-name files #"\d{13}.edn")]
      (let [parsed (clojure.edn/read-string (slurp f))]
        (swap! state update-in [:entries] conj parsed)))
    {:state state}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:geo-entry/persist geo-entry-persist-fn
                 :state/get         state-get-fn}})

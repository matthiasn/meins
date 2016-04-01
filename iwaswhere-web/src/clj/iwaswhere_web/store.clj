(ns iwaswhere-web.store
  (:require [iwaswhere-web.imports :as i]
            [iwaswhere-web.files :as f]
            [clojure.pprint :as pp]))

(defn state-get-fn
  "Handler function for retrieving current state."
  [{:keys [current-state]}]
  {:emit-msg [:state/new current-state]})

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state."
  [_put-fn]
  (let [state (atom {:entries []})
        files (file-seq (clojure.java.io/file "./data"))]
    (doseq [f (f/filter-by-name files #"\d{13}.edn")]
      (let [parsed (clojure.edn/read-string (slurp f))]
        (swap! state update-in [:entries] conj parsed)))
    {:state state}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:geo-entry/persist  f/geo-entry-persist-fn
                 :text-entry/persist f/geo-entry-persist-fn
                 :state/get          state-get-fn
                 :import/photos      i/import-photos}})

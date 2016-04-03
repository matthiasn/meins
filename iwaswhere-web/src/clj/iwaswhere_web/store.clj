(ns iwaswhere-web.store
  (:require [iwaswhere-web.imports :as i]
            [iwaswhere-web.files :as f]
            [ubergraph.core :as uber]
            [clojure.pprint :as pp]))

(defn state-get-fn
  "Handler function for retrieving current state."
  [{:keys [current-state]}]
  {:emit-msg [:state/new {:entries (vals (:entries-map current-state))}]})

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state."
  [_put-fn]
  (let [state (atom {:entries-map (sorted-map)})
        files (file-seq (clojure.java.io/file "./data"))]
    (doseq [f (f/filter-by-name files #"\d{13}.edn")]
      (let [parsed (clojure.edn/read-string (slurp f))
            ts (:timestamp parsed)]
        (swap! state assoc-in [:entries-map ts] parsed)))
    {:state state}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:geo-entry/persist  f/geo-entry-persist-fn
                 :text-entry/persist f/geo-entry-persist-fn
                 :state/get          state-get-fn
                 :import/photos      i/import-photos}})

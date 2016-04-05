(ns iwaswhere-web.store
  (:require [iwaswhere-web.imports :as i]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.graph :as g]
            [ubergraph.core :as uber]
            [clojure.pprint :as pp]))

(defn state-get-fn
  "Handler function for retrieving current state."
  [{:keys [current-state]}]
  {:emit-msg [:state/new (g/extract-sorted-entries current-state)]})

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state.
  Entries are stored as attributes of graph nodes, where the node itself is
  timestamp of an entry. A sort order by descending timestamp is maintained
  in a sorted set of the nodes."
  [path]
  (fn
    [_put-fn]
    (let [state (atom {:sorted-entries (sorted-set-by >)
                       :graph          (uber/graph)})
          files (file-seq (clojure.java.io/file path))]
      (doseq [f (f/filter-by-name files #"\d{13}.edn")]
        (let [parsed (clojure.edn/read-string (slurp f))
              ts (:timestamp parsed)]
          (swap! state g/add-node ts parsed)))
      {:state state})))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (state-fn "./data")
   :handler-map {:geo-entry/persist  f/geo-entry-persist-fn
                 :text-entry/persist f/geo-entry-persist-fn
                 :state/get          state-get-fn
                 :import/photos      i/import-photos}})

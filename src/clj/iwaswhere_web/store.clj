(ns iwaswhere-web.store
  "This namespace contains the functions necessary to instantiate the store-cmp,
   which then holds the server side application state."
  (:require [iwaswhere-web.files :as f]
            [taoensso.timbre.profiling :refer [p profile]]
            [iwaswhere-web.graph.query :as gq]
            [iwaswhere-web.graph.stats :as gs]
            [iwaswhere-web.graph.add :as ga]
            [iwaswhere-web.specs]
            [ubergraph.core :as uber]
            [iwaswhere-web.keepalive :as ka]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [iwaswhere-web.fulltext-search :as ft]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [iwaswhere-web.file-utils :as fu]))

(defn read-dir
  [state entries-to-index cfg]
  (let [path (:daily-logs-path (fu/paths))
        files (file-seq (clojure.java.io/file path))]
    (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn")]
      (with-open [reader (clojure.java.io/reader f)]
        (let [lines (line-seq reader)]
          (doseq [line lines]
            (try
              (let [parsed (clojure.edn/read-string line)
                    ts (:timestamp parsed)]
                (if (:deleted parsed)
                  (do (swap! state ga/remove-node ts)
                      (swap! entries-to-index dissoc ts))
                  (do (swap! entries-to-index assoc-in [ts] parsed)
                      (swap! state ga/add-node ts parsed :startup))))
              (catch Exception ex
                (log/error "Exception" ex "when parsing line:\n" line)))))))))

(defn ft-index
  [entries-to-index put-fn]
  (let [path (:clucy-path (fu/paths))
        files (file-seq (clojure.java.io/file path))
        clucy-dir-empty? (empty? (filter #(.isFile %) files))]
    (when clucy-dir-empty?
      (future
        (Thread/sleep 2000)
        (log/info "Fulltext-Indexing started")
        (let [t (with-out-str
                  (time (doseq [entry (vals @entries-to-index)]
                          (put-fn [:ft/add entry]))))]
          (log/info "Indexed" (count @entries-to-index) "entries." t))
        (reset! entries-to-index [])))))

(defn recreate-state
  "Creates state atom and then parses all files in data directory into the
   component state.
   Entries are stored as attributes of graph nodes, where the node itself is
   timestamp of an entry. A sort order by descending timestamp is maintained
   in a sorted set of the nodes timestamps."
  [put-fn]
  (let [conf (fu/load-cfg)
        entries-to-index (atom {})
        state (atom {:sorted-entries (sorted-set-by >)
                     :graph          (uber/graph)
                     :cfg            conf})
        t (with-out-str (time (read-dir state entries-to-index conf)))]
    (log/info "Read" (count @entries-to-index) "entries." t)
    (ft-index entries-to-index put-fn)
    {:state state}))

(defn state-fn
  "Initial state function. If persisted state exists, read that (much faster),
   otherwise recreate it from then append log. Should be deleted or renamed
   whenever there is an application update to avoid inconsistencies."
  [put-fn]
  (try
    (if (and (System/getenv "CACHED_APPSTATE")
             (fs/exists? (:app-cache (fu/paths))))
      (f/state-from-file)
      (recreate-state put-fn))
    (catch Exception ex (do (log/error "Error reading cache" ex)
                            (recreate-state put-fn)))))

(defn refresh-cfg
  "Refresh configuration by reloading the config file."
  [{:keys [current-state]}]
  {:new-state (assoc-in current-state [:cfg] (fu/load-cfg))})

(defn cmp-map
  "Generates component map for state-cmp."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :opts        {:msgs-on-firehose true}
   :handler-map (merge
                  gs/stats-handler-map
                  {:entry/import   f/entry-import-fn
                   :entry/find     gq/find-entry
                   :entry/update   f/geo-entry-persist-fn
                   :entry/trash    f/trash-entry-fn
                   :state/search   gq/query-fn
                   :cfg/refresh    refresh-cfg
                   :cmd/keep-alive ka/keepalive-fn})})

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
            [clojure.pprint :as pp]))

(defn publish-state-fn
  "Publishes current state, as filtered for the respective clients. Sends to
   single connected client with the latest filter when message payload contains
   :sente-uid, otherwise sends to all clients."
  [{:keys [current-state msg-payload msg-meta]}]
  (if-let [sente-uid (:sente-uid msg-payload)]
    (let [start-ts (System/nanoTime)
          query (get-in current-state [:client-queries sente-uid])
          res (gq/get-filtered-results current-state query)
          ms (/ (- (System/nanoTime) start-ts) 1000000)
          ms-string (pp/cl-format nil "~,3f ms" ms)
          res-msg (with-meta [:state/new (merge res {:duration-ms ms-string})]
                             (merge msg-meta {:sente-uid sente-uid}))]
      (log/info "Query" sente-uid "took" ms-string)
      (log/info "Result size" (count (str res)))
      {:emit-msg res-msg})
    {:send-to-self (map (fn [uid] [:state/publish-current {:sente-uid uid}])
                        (keys (:client-queries current-state)))}))

(defn state-get-fn
  "Handler function for retrieving current state. Updates filter for connected
   client, and then sends a message to self to publish state for this particular
   client.
   Removes '~' from the not-tags, which is the set of tags that shall not be
   contained in matching entries or any of their comments."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)]
    {:new-state    (update-in current-state [:client-queries sente-uid]
                              merge msg-payload)
     :send-to-self [(with-meta [:state/publish-current {:sente-uid sente-uid}]
                               msg-meta)
                    (with-meta [:cmd/keep-alive] msg-meta)]}))

(defn stats-tags-fn
  "Precomputes stats and tags (they only change on insert anyway)."
  [{:keys [current-state]}]
  {:new-state (-> current-state
                  (assoc-in [:stats] (gs/get-basic-stats current-state))
                  (assoc-in [:hashtags] (gq/find-all-hashtags current-state))
                  (assoc-in [:mentions] (gq/find-all-mentions current-state)))})

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state.
  Entries are stored as attributes of graph nodes, where the node itself is
  timestamp of an entry. A sort order by descending timestamp is maintained
  in a sorted set of the nodes."
  [path]
  (fn
    [_put-fn]
    (fs/mkdirs f/daily-logs-path)
    (let [entries-to-index (atom [])
          state (atom {:sorted-entries (sorted-set-by >)
                       :graph          (uber/graph)
                       ;:lucene-index   (clucy/memory-index)
                       :lucene-index   ft/index
                       :client-queries {}
                       :hashtags       #{}
                       :mentions       #{}
                       :stats          {:entry-count 0
                                        :node-count  0
                                        :edge-count  0}})
          files (file-seq (clojure.java.io/file path))]
      (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn")]
        (with-open [reader (clojure.java.io/reader f)]
          (let [lines (line-seq reader)]
            (doseq [line lines]
              (let [parsed (clojure.edn/read-string line)
                    ts (:timestamp parsed)]
                (if (:deleted parsed)
                  (swap! state ga/remove-node ts)
                  (do (swap! entries-to-index conj
                             (select-keys parsed [:timestamp :md]))
                      (swap! state ga/add-node ts parsed))))))))
      (future
        (let [t (with-out-str
                  (time (doseq [entry @entries-to-index]
                          (ft/add-to-index (:lucene-index @state) entry))))]
          (log/info "Indexed" (count @entries-to-index) "entries." t))
        (reset! entries-to-index []))
      ; nicer would be: send off :state/stats-tags message
      (swap! state #(:new-state (stats-tags-fn {:current-state %})))
      {:state state})))

(defn cmp-map
  "Generates component map for state-cmp."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (state-fn f/daily-logs-path)
   :handler-map {:entry/import           f/entry-import-fn
                 :entry/update           f/geo-entry-persist-fn
                 :entry/trash            f/trash-entry-fn
                 :state/publish-current  publish-state-fn
                 :state/get              state-get-fn
                 :state/stats-tags       stats-tags-fn
                 :cmd/keep-alive         ka/keepalive-fn
                 :cmd/query-gc           ka/query-gc-fn
                 :stats/pomo-day-get     gs/get-pomodoro-day-stats
                 :stats/activity-day-get gs/get-activity-day-stats}})

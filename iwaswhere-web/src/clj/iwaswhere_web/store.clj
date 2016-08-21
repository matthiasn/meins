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
    {:send-to-self (mapv (fn [uid] [:state/publish-current {:sente-uid uid}])
                         (keys (:client-queries current-state)))}))

(defn run-query
  [current-state msg-meta]
  (fn
    [[query-id query]]
    (let [start-ts (System/nanoTime)
          res (gq/get-filtered-results current-state query)
          ms (/ (- (System/nanoTime) start-ts) 1000000)
          dur {:duration-ms (pp/cl-format nil "~,3f ms" ms)}]
      (log/info "Query" (:sente-uid msg-meta) query-id "took" (:duration-ms dur))
      (log/info "Result size" (count (str res)))
      (with-meta [:state/new (merge res dur {:query-id query-id})] msg-meta))))

(defn publish-state-fn
  "Publishes current state, as filtered for the respective clients. Sends to
   single connected client with the latest filter when message payload contains
   :sente-uid, otherwise sends to all clients."
  [{:keys [current-state msg-payload msg-meta]}]
  (if-let [sente-uid (:sente-uid msg-payload)]
    (let [queries (get-in current-state [:client-queries sente-uid :queries])
          msg-meta  (merge msg-meta {:sente-uid sente-uid})
          get-results (run-query current-state (merge msg-meta {:sente-uid sente-uid}))
          results (mapv get-results queries)]
      {:emit-msg results})
    {:send-to-self (mapv (fn [uid] [:state/publish-current {:sente-uid uid}])
                         (keys (:client-queries current-state)))}))

(defn state-get-fn
  "Handler function for retrieving current state. Updates filter for connected
   client, and then sends a message to self to publish state for this particular
   client.
   Removes '~' from the not-tags, which is the set of tags that shall not be
   contained in matching entries or any of their comments."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)
        query-id (:query-id msg-payload)
        update-path [:client-queries sente-uid :queries query-id]
        new-state (update-in current-state update-path merge msg-payload)
        publish-msg [:state/publish-current {:sente-uid sente-uid}]]
    {:new-state new-state
     :send-to-self [(with-meta publish-msg msg-meta)
                    (with-meta [:cmd/keep-alive] msg-meta)]}))

(defn stats-tags-fn
  "Precomputes stats and tags (they only change on insert anyway) and initiates
   publication thereof to all connected clients."
  [{:keys [current-state]}]
  (let [new-state
        (-> current-state
            (assoc-in [:stats] (gs/get-basic-stats current-state))
            (assoc-in [:hashtags] (gq/find-all-hashtags current-state))
            (assoc-in [:pvt-hashtags] (gq/find-all-pvt-hashtags current-state))
            (assoc-in [:mentions] (gq/find-all-mentions current-state))
            (assoc-in [:activities] (gq/find-all-activities current-state))
            (assoc-in [:consumption-types]
                      (gq/find-all-consumption-types current-state)))]
    {:new-state    new-state
     :send-to-self (mapv (fn [uid]
                           (with-meta [:state/stats-tags-get] {:sente-uid uid}))
                         (keys (:client-queries current-state)))}))

(defn publish-stats-tags
  "Publish stats and tags to client."
  [{:keys [current-state msg-meta]}]
  (let [stats-tags {:hashtags          (:hashtags current-state)
                    :pvt-hashtags      (:pvt-hashtags current-state)
                    :mentions          (:mentions current-state)
                    :activities        (:activities current-state)
                    :consumption-types (:consumption-types current-state)
                    :stats             (:stats current-state)}]
    {:emit-msg [:state/stats-tags stats-tags]}))

(defn state-fn
  "Initial state function, creates state atom and then parses all files in
   data directory into the component state.
   Entries are stored as attributes of graph nodes, where the node itself is
   timestamp of an entry. A sort order by descending timestamp is maintained
   in a sorted set of the nodes."
  [put-fn]
  (fs/mkdirs f/daily-logs-path)
  (let [entries-to-index (atom {})
        state (atom {:sorted-entries (sorted-set-by >)
                     :graph          (uber/graph)
                     :lucene-index   ft/index
                     :client-queries {}
                     :hashtags       #{}
                     :mentions       #{}
                     :stats          {:entry-count 0
                                      :node-count  0
                                      :edge-count  0}})
        files (file-seq (clojure.java.io/file f/daily-logs-path))]
    (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn")]
      (with-open [reader (clojure.java.io/reader f)]
        (let [lines (line-seq reader)]
          (doseq [line lines]
            (let [parsed (clojure.edn/read-string line)
                  ts (:timestamp parsed)]
              (if (:deleted parsed)
                (do (swap! state ga/remove-node ts)
                    (swap! entries-to-index dissoc ts))
                (do (swap! entries-to-index assoc-in [ts] parsed)
                    (swap! state ga/add-node ts parsed))))))))

    (future
      (Thread/sleep 10000)
      (log/info "Indexing started")
      (let [t (with-out-str
                (time (doseq [entry (vals @entries-to-index)]
                        (put-fn [:ft/add entry]))))]
        (log/info "Indexed" (count @entries-to-index) "entries." t))
      (reset! entries-to-index []))
    ; TODO: send off :state/stats-tags message
    (swap! state #(:new-state (stats-tags-fn {:current-state %})))
    {:state state}))

(defn cmp-map
  "Generates component map for state-cmp."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :opts        {:msgs-on-firehose true}
   :handler-map {:entry/import           f/entry-import-fn
                 :entry/find             gq/find-entry
                 :entry/update           f/geo-entry-persist-fn
                 :entry/trash            f/trash-entry-fn
                 :state/publish-current  publish-state-fn
                 :state/get              state-get-fn
                 :state/stats-tags-make  stats-tags-fn
                 :state/stats-tags-get   publish-stats-tags
                 :cmd/keep-alive         ka/keepalive-fn
                 :cmd/query-gc           ka/query-gc-fn
                 :stats/pomo-day-get     gs/get-pomodoro-day-stats
                 :stats/activity-day-get gs/get-activity-day-stats
                 :stats/tasks-day-get    gs/get-tasks-day-stats}})

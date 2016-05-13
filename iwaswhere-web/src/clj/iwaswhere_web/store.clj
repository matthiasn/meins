(ns iwaswhere-web.store
  (:require [iwaswhere-web.files :as f]
            [iwaswhere-web.graph :as g]
            [ubergraph.core :as uber]
            [clojure.string :as s]
            [iwaswhere-web.keepalive :as ka]))

(defn publish-state-fn
  "Publishes current state, as filtered by the respective clients. Sends to single connected client
  with the latest filter when message payload contains :sente-uid, otherwise sends to all clients."
  [{:keys [current-state msg-payload]}]
  (let [sente-uid (:sente-uid msg-payload)
        sente-uids (if sente-uid [sente-uid] (keys (:last-filter current-state)))
        state-emit-mapper (fn [sente-uid]
                            (let [filter (get-in current-state [:last-filter sente-uid])]
                              (with-meta [:state/new (g/get-filtered-results current-state filter)]
                                         {:sente-uid sente-uid})))]
    {:emit-msgs (map state-emit-mapper sente-uids)}))

(defn state-get-fn
  "Handler function for retrieving current state. Updates filter for connected client, and then
  sends a message to self to publish state for this particular client.
  Removes '~' from the not-tags, which is the set of tags that shall not be contained
  in matching entries or any of their comments."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [sente-uid (:sente-uid msg-meta)
        query (update-in msg-payload [:not-tags] (fn [not-tags] (set (map #(s/replace % #"~" "") not-tags))))
        new-state (assoc-in current-state [:last-filter sente-uid] query)]
    {:new-state new-state
     :send-to-self [:state/publish-current {:sente-uid sente-uid}]}))

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
                       :graph          (uber/graph)
                       :last-filter    {}})
          files (file-seq (clojure.java.io/file path))]
      (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}.jrn")]
        (let [lines (line-seq (clojure.java.io/reader f))]
          (doseq [line lines]
            (let [parsed (clojure.edn/read-string line)
                  ts (:timestamp parsed)]
              (if (:deleted parsed)
                (swap! state g/remove-node ts)
                (swap! state g/add-node ts parsed))))))
      {:state state})))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (state-fn "./data/daily-logs")
   :handler-map {:geo-entry/persist     f/geo-entry-persist-fn
                 :geo-entry/import      f/entry-import-fn
                 :text-entry/update     f/geo-entry-persist-fn
                 :state/publish-current publish-state-fn
                 :cmd/trash             f/trash-entry-fn
                 :state/get             state-get-fn
                 :cmd/keep-alive        ka/keepalive-fn
                 :cmd/query-gc          ka/query-gc-fn}})

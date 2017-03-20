(ns iwaswhere-web.files
  "This namespace takes care of persisting new entries to the log files.
  All changes to the graph data structure are appended to a daily log.
  Then later on application startup, all these state changes can be
  replayed to recreate the application state. This mechanism is inspired
  by Event Sourcing (http://martinfowler.com/eaaDev/EventSourcing.html)."
  (:require [iwaswhere-web.graph.add :as ga]
            [clj-uuid :as uuid]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uber]
            [matthiasn.systems-toolbox.component :as st]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.tools.logging :as l]))

(def data-path (or (System/getenv "DATA_PATH")
                   (let [path (str (System/getenv "HOME") "/iWasWhere/data")]
                     (when (fs/exists? path) path))
                   "data"))
(def daily-logs-path (str data-path "/daily-logs/"))

(defn paths
  [cfg custom-path-key]
  (let [custom-path (get-in cfg [:custom-data-paths custom-path-key :path])
        data-path (or (get-in cfg [:custom-data-paths custom-path :path])
                      data-path)
        daily-logs-path (if custom-path
                          (str custom-path "/daily-logs/")
                          daily-logs-path)
        trash-path (str data-path "/trash/")]
    (fs/mkdirs daily-logs-path)
    (fs/mkdirs trash-path)
    {:data-path       data-path
     :daily-logs-path daily-logs-path
     :trash-path      trash-path}))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn append-daily-log
  "Appends journal entry to the current day's log file."
  [cfg entry]
  (let [filename (str (:daily-logs-path (paths cfg (:custom-path entry)))
                      (tf/unparse (tf/formatters :year-month-day) (time/now))
                      ".jrn")
        serialized (str (pr-str entry) "\n")]
    (spit filename serialized :append true)))

(defn entry-import-fn
  "Handler function for persisting an imported journal entry."
  [{:keys [current-state msg-payload]}]
  (let [id (or (:id msg-payload) (uuid/v1))
        entry (merge msg-payload {:last-saved (st/now) :id id})
        ts (:timestamp entry)
        graph (:graph current-state)
        exists? (uber/has-node? graph ts)
        existing (when exists? (uber/attrs graph ts))
        node-to-add (if exists?
                      (if (= (:md existing) "No departure recorded #visit")
                        entry
                        (merge existing
                               (select-keys msg-payload [:longitude
                                                         :latitude
                                                         :horizontal-accuracy
                                                         :gps-timestamp
                                                         :linked-entries])))
                      entry)
        cfg (:cfg current-state)]
    (when-not (= existing node-to-add)
      (append-daily-log cfg node-to-add))
    {:new-state (ga/add-node current-state ts node-to-add false)
     :emit-msg  [[:ft/add entry]
                 [:cmd/schedule-new
                  {:timeout 5000
                   :message (with-meta [:search/refresh]
                                       {:sente-uid :broadcast})}]]}))

(defn geo-entry-persist-fn
  "Handler function for persisting journal entry."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [ts (:timestamp msg-payload)
        id (or (:id msg-payload) (uuid/v1))
        entry (merge msg-payload {:last-saved (st/now) :id id})
        new-state (ga/add-node current-state ts entry false)
        cfg (:cfg current-state)]
    (when (not= current-state new-state)
      (append-daily-log cfg entry))
    {:new-state    new-state
     :send-to-self (when-let [comment-for (:comment-for msg-payload)]
                     (with-meta [:entry/find {:timestamp comment-for}] msg-meta))
     :emit-msg     [[:entry/saved entry]
                    [:ft/add entry]
                    [:cmd/schedule-new {:message [:state/stats-tags-get]
                                        :timeout 200}]]}))

(defn move-attachment-to-trash
  "Moves attached media file to trash folder."
  [cfg entry dir k]
  (when-let [filename (k entry)]
    (let [{:keys [data-path trash-path]} (paths cfg (:custom-path entry))]
      (fs/rename (str data-path "/" dir "/" filename)
                 (str trash-path filename))
      (l/info "Moved file to trash:" filename))))

(defn trash-entry-fn
  "Handler function for deleting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (ga/remove-node current-state entry-ts)
        cfg (:cfg current-state)]
    (log/info "Entry" entry-ts "marked as deleted.")
    (append-daily-log cfg {:timestamp (:timestamp msg-payload)
                           :deleted   true})
    (move-attachment-to-trash cfg msg-payload "images" :img-file)
    (move-attachment-to-trash cfg msg-payload "audio" :audio-file)
    (move-attachment-to-trash cfg msg-payload "videos" :video-file)
    {:new-state new-state
     :emit-msg  [[:ft/remove {:timestamp entry-ts}]
                 [:search/refresh]]}))

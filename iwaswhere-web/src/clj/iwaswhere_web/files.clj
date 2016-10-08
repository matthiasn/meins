(ns iwaswhere-web.files
  "This namespace takes care of persisting new entries to the log files.
  All changes to the graph data structure are appended to a daily log.
  Then later on application startup, all these state changes can be
  replayed to recreate the application state. This mechanism is inspired
  by Event Sourcing (http://martinfowler.com/eaaDev/EventSourcing.html)."
  (:require [iwaswhere-web.graph.add :as ga]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [clojure.tools.logging :as log]
            [iwaswhere-web.fulltext-search :as ft]
            [ubergraph.core :as uber]
            [matthiasn.systems-toolbox.component :as st]
            [me.raynes.fs :as fs]
            [clojure.tools.logging :as l]))

(def data-path (or (System/getenv "DATA_PATH")
                   (let [path (str (System/getenv "HOME") "/iWasWhere/data")]
                     (when (fs/exists? path) path))
                   "data"))
(def daily-logs-path (str data-path "/daily-logs/"))
(def trash-path (str data-path "/trash/"))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn append-daily-log
  "Appends journal entry to the current day's log file."
  [entry]
  (let [filename (str daily-logs-path
                      (timef/unparse (timef/formatters :year-month-day)
                                     (time/now)) ".jrn")
        serialized (str (pr-str entry) "\n")]
    (spit filename serialized :append true)))

(defn entry-import-fn
  "Handler function for persisting an imported journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry (merge msg-payload {:last-saved (st/now)})
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
                      entry)]
    (when-not (= existing node-to-add)
      (append-daily-log node-to-add))
    {:new-state (ga/add-node current-state ts node-to-add)
     :emit-msg  [[:ft/add entry]
                 [:cmd/schedule-new
                  {:timeout 5000
                   :message (with-meta [:search/refresh]
                                       {:sente-uid :broadcast})}]]}))

(defn geo-entry-persist-fn
  "Handler function for persisting journal entry."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [ts (:timestamp msg-payload)
        entry (merge msg-payload {:last-saved (st/now)})
        new-state (ga/add-node current-state ts entry)]
    (append-daily-log entry)
    {:new-state    new-state
     :send-to-self (when-let [comment-for (:comment-for msg-payload)]
                     (with-meta [:entry/find {:timestamp comment-for}] msg-meta))
     :emit-msg     [[:entry/saved entry]
                    [:ft/add entry]
                    [:cmd/schedule-new {:timeout 2000
                                        :message [:state/stats-tags-get]}]]}))

(defn move-attachment-to-trash
  "Moves attached media file to trash folder."
  [entry dir k]
  (when-let [filename (k entry)]
    (fs/rename (str data-path "/" dir "/" filename)
               (str data-path "/trash/" filename))
    (l/info "Moved file to trash:" filename)))

(defn trash-entry-fn
  "Handler function for deleting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (ga/remove-node current-state entry-ts)]
    (log/info "Entry" entry-ts "marked as deleted.")
    (append-daily-log {:timestamp (:timestamp msg-payload)
                       :deleted   true})
    (fs/mkdirs trash-path)
    (move-attachment-to-trash msg-payload "images" :img-file)
    (move-attachment-to-trash msg-payload "audio" :audio-file)
    (move-attachment-to-trash msg-payload "videos" :video-file)
    {:new-state new-state
     :emit-msg  [[:ft/remove {:timestamp entry-ts}]
                 [:search/refresh]]}))

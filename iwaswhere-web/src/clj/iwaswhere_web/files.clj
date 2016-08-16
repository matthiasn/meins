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
            [matthiasn.systems-toolbox.component :as st]))

(def data-path (or (System/getenv "DATA_PATH") "data"))
(def daily-logs-path (str data-path "/daily-logs/"))

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
  (let [entry-ts (:timestamp msg-payload)
        graph (:graph current-state)
        exists? (uber/has-node? graph entry-ts)
        existing (when exists? (uber/attrs graph entry-ts))
        node-to-add (if exists?
                      (if (= (:md existing) "No departure recorded #visit")
                        msg-payload
                        (merge existing
                               (select-keys msg-payload [:longitude
                                                         :latitude
                                                         :horizontal-accuracy
                                                         :gps-timestamp])))
                      msg-payload)]
    (when-not (= existing node-to-add)
      (append-daily-log node-to-add))
    {:new-state (ga/add-node current-state entry-ts node-to-add)}))

(defn geo-entry-persist-fn
  "Handler function for persisting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        with-last-modified (merge msg-payload {:last-saved (st/now)})
        new-state (ga/add-node current-state entry-ts with-last-modified)]
    (append-daily-log with-last-modified)
    {:new-state    new-state
     :emit-msg     [[:entry/saved with-last-modified]
                    [:ft/add with-last-modified]]
     :send-to-self [[:state/publish-current {}]
                    [:state/stats-tags-make]]}))

(defn trash-entry-fn
  "Handler function for deleting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (ga/remove-node current-state entry-ts)]
    (log/info "Entry" entry-ts "marked as deleted.")
    (append-daily-log (merge msg-payload {:deleted true}))
    {:new-state    new-state
     :send-to-self [[:state/publish-current {}]
                    [:state/stats-tags-make]]
     :emit-msg     [:ft/remove {:timestamp entry-ts}]}))

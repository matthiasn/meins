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
            [matthiasn.systems-toolbox.component :as st]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.logging :as l]
            [clojure.pprint :as pp]
            [iwaswhere-web.file-utils :as fu]
            [iwaswhere-web.location :as loc]
            [ubergraph.core :as uc]))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn append-daily-log
  "Appends journal entry to the current day's log file."
  [cfg entry]
  (let [filename (str (:daily-logs-path (fu/paths))
                      (tf/unparse (tf/formatters :year-month-day) (time/now))
                      ".jrn")
        serialized (str (pr-str entry) "\n")]
    (spit filename serialized :append true)))

(defn entry-import-fn
  "Handler function for persisting an imported journal entry."
  [{:keys [current-state msg-payload]}]
  (let [id (or (:id msg-payload) (uuid/v1))
        entry (merge msg-payload {:last-saved (st/now) :id id})
        entry (loc/enrich-geoname entry)
        ts (:timestamp entry)
        graph (:graph current-state)
        exists? (uc/has-node? graph ts)
        existing (when exists? (uc/attrs graph ts))
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
     :emit-msg  [[:ft/add entry]]}))

(defn geo-entry-persist-fn
  "Handler function for persisting journal entry."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [ts (:timestamp msg-payload)
        id (or (:id msg-payload) (uuid/v1))
        entry (merge msg-payload {:last-saved (st/now) :id id})
        entry (loc/enrich-geoname entry)
        new-state (ga/add-node current-state ts entry false)
        cfg (:cfg current-state)
        g (:graph current-state)
        prev (when (uc/has-node? g ts) (uc/attrs g ts))]
    (when (not= (dissoc prev :last-saved)
                (dissoc entry :last-saved))
      (append-daily-log cfg entry))
    {:new-state    new-state
     :send-to-self (when-let [comment-for (:comment-for msg-payload)]
                     (with-meta [:entry/find {:timestamp comment-for}] msg-meta))
     :emit-msg     [(with-meta [:entry/saved entry] {:sente-uid :broadcast})
                    [:ft/add entry]]}))

(defn move-attachment-to-trash
  "Moves attached media file to trash folder."
  [cfg entry dir k]
  (when-let [filename (k entry)]
    (let [{:keys [data-path trash-path]} (fu/paths)]
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

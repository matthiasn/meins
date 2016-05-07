(ns iwaswhere-web.files
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.graph :as g]
            [clj-time.core :as time]
            [clj-time.format :as timef]
            [me.raynes.fs :as fs]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uber]))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn append-daily-log
  "Appends journal entry to the current day's log file."
  [entry]
  (let [filename (str "./data/daily-logs/" (timef/unparse (timef/formatters :year-month-day) (time/now)) ".jrn")
        serialized (str (pr-str entry) "\n")]
    (spit filename serialized :append true)))

(defn entry-import-fn
  "Handler function for persisting an imported journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        last-filter (:last-filter current-state)
        graph (:graph current-state)
        exists? (uber/has-node? graph entry-ts)
        existing (when exists? (uber/attrs graph entry-ts))]
    ; Okay this is slightly too specific for my taste, but currently, the completion
    ; of a visit is an update to a visit, and otherwise, the exists? logic would refuse
    ; to import it.
    (if (and exists? (not= (:md existing) "No departure recorded #visit"))
      (log/warn "Entry exists, skipping" msg-payload)
      (let [new-state (g/add-node current-state entry-ts msg-payload)]
        (append-daily-log msg-payload)
        {:new-state new-state
         :emit-msg  [:state/new (g/get-filtered-results new-state last-filter)]}))))

(defn geo-entry-persist-fn
  "Handler function for persisting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        last-filter (:last-filter current-state)
        new-state (g/add-node current-state entry-ts msg-payload)]
    (append-daily-log msg-payload)
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state last-filter)]}))

(defn trash-entry-fn
  "Handler function for deleting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        last-filter (:last-filter current-state)
        new-state (g/remove-node current-state entry-ts)]
    (log/info "Entry" entry-ts "marked as deleted.")
    (append-daily-log (merge msg-payload {:deleted true}))
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state last-filter)]}))

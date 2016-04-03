(ns iwaswhere-web.files
  (:require [clojure.pprint :as pp]))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn geo-entry-persist-fn
  "Handler function for persisting new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (assoc-in current-state [:entries-map entry-ts] msg-payload)
        filename (str "./data/" (:timestamp msg-payload) ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg  [:state/new {:entries (vals (:entries-map new-state))}]}))
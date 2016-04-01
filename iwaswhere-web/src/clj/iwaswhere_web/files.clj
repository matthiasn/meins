(ns iwaswhere-web.files
  (:require [clojure.pprint :as pp]))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn geo-entry-persist-fn
  "Handler function for persisting new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [new-state (update-in current-state [:entries] conj msg-payload)
        filename (str "./data/" (:timestamp msg-payload) ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg  [:state/new new-state]}))
(ns iwaswhere-web.files
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.graph :as g]
            [me.raynes.fs :as fs]
            [clojure.tools.logging :as log]))

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

(defn geo-entry-persist-fn
  "Handler function for persisting new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (-> current-state
                      (g/add-node entry-ts msg-payload)
                      (assoc-in [:last-filter] msg-payload))
        filename (str "./data/" entry-ts ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state msg-payload)]}))

(defn geo-entry-update-fn
  "Handler function for updating new journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        last-filter (:last-filter current-state)
        new-state (g/add-node current-state entry-ts msg-payload)
        filename (str "./data/" entry-ts ".edn")]
    (spit filename (with-out-str (pp/pprint msg-payload)))
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state last-filter)]}))

(defn trash-entry-fn
  "Handler function for deleting journal entry."
  [{:keys [current-state msg-payload]}]
  (let [entry-ts (:timestamp msg-payload)
        new-state (g/remove-node current-state entry-ts)
        filename (str entry-ts ".edn")]
    (log/info "Moving file" filename "into trash folder.")
    (fs/rename (str "./data/" filename) (str "data/trash/" filename))
    {:new-state new-state
     :emit-msg  [:state/new (g/get-filtered-results new-state {})]}))

(ns iwaswhere-web.imports.screenshot
  "This namespace does imports, for example of photos."
  (:require [clojure.pprint :as pp]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.logging :as log]
            [iwaswhere-web.file-utils :as fu]))


(defn import-screenshot [{:keys [put-fn msg-meta msg-payload]}]
  (let [filename (str fu/data-path "/images/" (:filename msg-payload))]
    (log/info "importing screenshot" filename)
    (sh "/usr/sbin/screencapture" filename))
  {:emit-msg [:cmd/schedule-new
              {:timeout 3000 :message (with-meta [:search/refresh] msg-meta)}]})

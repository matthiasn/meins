(ns iwaswhere-web.upload
  "Provides upload via REST call."
  (:require [ring.adapter.jetty :as j]
            [compojure.core :refer [routes POST]]
            [clojure.java.io :as io]
            [iwaswhere-web.imports :as i]
            [clojure.string :as s]))

(defn state-fn
  "Fires up REST endpoint that accepts import files:
    - /upload/text-entry.json
    - /upload/visits.json"
  [put-fn]
  (let [post-fn (fn [filename req put-fn]
                  (with-open [rdr (io/reader (:body req))]
                    (case filename
                      "text-entries.json" (i/import-text-entries-fn rdr put-fn {} filename)
                      "visits.json" (i/import-visits-fn rdr put-fn {} filename)
                      :default)
                    "OK"))
        app (routes (POST "/upload/:filename" [filename :as r] (post-fn filename r put-fn)))]
    (j/run-jetty app {:port 3001 :join? false}))
  {:state (atom {})})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

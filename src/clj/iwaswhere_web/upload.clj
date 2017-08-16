(ns iwaswhere-web.upload
  "Provides upload via REST call."
  (:require [ring.adapter.jetty :as j]
            [compojure.core :refer [routes POST PUT]]
            [clojure.java.io :as io]
            [iwaswhere-web.imports :as i]
            [iwaswhere-web.files :as f]
            [image-resizer.util :refer :all]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [iwaswhere-web.file-utils :as fu]))

(def upload-port (Integer/parseInt (get (System/getenv) "UPLOAD_PORT" "3001")))

(defn start-server
  "Fires up REST endpoint that accepts import files:
    - /upload/text-entry.json
    - /upload/visits.json

   Then schedules shutdown."
  [{:keys [put-fn cmp-state current-state]}]
  (when-let [server (:server current-state)]
    (log/info "Stopping Upload Server")
    (.stop server))
  (log/info "Starting Upload Server")
  (let [post-fn (fn [filename req put-fn]
                  (with-open [rdr (io/reader (:body req))]
                    (case filename
                      "text-entries.json" (i/import-text-entries-fn
                                            rdr put-fn {} filename)
                      "visits.json" (i/import-visits-fn rdr put-fn {} filename)
                      (prn req))
                    "OK"))
        binary-post-fn (fn [dir filename req]
                         (let [filename (str fu/data-path "/" dir "/" filename)
                               file (java.io.File. filename)]
                           (io/make-parents file)
                           (log/info :server/upload-cmp req)
                           (io/copy (:body req) file))
                         "OK")
        app (routes
              (PUT "/upload/:dir/:file" [dir file :as r]
                (binary-post-fn dir file r))
              (POST "/upload/:filename" [filename :as r]
                (post-fn filename r put-fn)))
        server (j/run-jetty app {:port upload-port :join? false})]
    {:new-state (assoc-in current-state [:server] server)
     :emit-msg  [:cmd/schedule-new
                 {:timeout 120000
                  :message [:import/stop-server]}]}))

(defn stop-server
  [{:keys [current-state]}]
  (log/info "Stopping Upload Server")
  (.stop (:server current-state))
  {:new-state (assoc-in current-state [:server] nil)})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:import/listen      start-server
                 :import/stop-server stop-server}})


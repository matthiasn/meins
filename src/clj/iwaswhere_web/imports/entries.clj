(ns iwaswhere-web.imports.entries
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.migrations :as m]
            [iwaswhere-web.imports.screenshot :as is]
            [iwaswhere-web.imports.spotify :as iss]
            [iwaswhere-web.specs :as specs]
            [iwaswhere-web.imports.flight :as fl]
            [iwaswhere-web.imports.media :as im]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [cheshire.core :as cc]
            [camel-snake-kebab.core :refer :all]
            [clj-time.coerce :as c]
            [me.raynes.fs :as fs]
            [clj-http.client :as hc]
            [clj-time.format :as tf]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.file-utils :as fu]
            [clj-time.format :as ctf]))

(defn import-visits-fn
  [rdr put-fn msg-meta filename]
  (try
    (let [lines (line-seq rdr)]
      (doseq [line lines]
        (let [raw-visit (cc/parse-string line #(keyword (s/replace % "_" "-")))
              {:keys [arrival-ts departure-ts]} (u/visit-timestamps raw-visit)
              dur (when departure-ts
                    (-> (- departure-ts arrival-ts)
                        (/ 6000)
                        (Math/floor)
                        (/ 10)))
              visit (merge raw-visit
                           {:timestamp arrival-ts
                            :md        (if dur
                                         (str "Duration: " dur "m #visit")
                                         "No departure recorded #visit")
                            :tags      #{"#visit" "#import"}})]
          (if-not (neg? (:timestamp visit))
            (put-fn (with-meta [:entry/import visit] msg-meta))
            (log/warn "negative timestamp?" visit)))))
    (catch Exception ex (log/error "Error while importing " filename ex))))

(defn import-geo
  "Imports geo data from respective directory.
  For now, only pays attention to visits."
  [{:keys [put-fn msg-meta]}]
  (let [files (file-seq (io/file (str fu/data-path "/geo-import")))]
    (log/info "importing photos")
    (doseq [file (f/filter-by-name files #"visits.json" #_#"[-0-9]+.(json)")]
      (let [filename (.getName file)]
        (log/info "Trying to import " filename)
        (import-visits-fn (io/reader file) put-fn msg-meta filename)))))

(defn update-audio-tag
  [entry]
  (if (:audio-file entry)
    (-> entry
        (update-in [:tags] conj "#audio")
        (update-in [:md] str " #audio"))
    entry))

(defn import-text-entries-fn
  [rdr put-fn msg-meta filename]
  (try (let [lines (line-seq rdr)]
         (doseq [line lines]
           (when (seq line)
             (let [set-linked
                   (fn [entry]
                     (assoc-in entry [:linked-entries]
                               (when-let [linked (:linked-timestamp entry)]
                                 #{linked})))
                   entry
                   (-> (cc/parse-string line #(keyword (s/replace % "_" "-")))
                       (m/add-tags-mentions)
                       (update-audio-tag)
                       (update-in [:timestamp] u/double-ts-to-long)
                       (update-in [:linked-timestamp] u/double-ts-to-long))
                   entry (if (:linked-timestamp entry)
                           (set-linked entry)
                           (update-in entry [:tags] conj "#import"))]
               (put-fn (with-meta [:entry/import entry] msg-meta))))))
       (catch Exception ex (log/error (str "Error while importing "
                                           filename) ex))))

(defn import-text-entries
  "Imports text entries from phone."
  [{:keys [put-fn msg-meta]}]
  (let [files (file-seq (io/file (str fu/data-path "/geo-import")))]
    (log/info "importing photos")
    (doseq [file (f/filter-by-name files #"text-entries.json")]
      (let [filename (.getName file)]
        (log/info "Trying to import " filename)
        (import-text-entries-fn (io/reader file) put-fn msg-meta filename)))
    {:emit-msg [:search/refresh]}))


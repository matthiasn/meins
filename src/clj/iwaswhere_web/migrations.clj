(ns iwaswhere-web.migrations
  "This namespace is used for migrating entries to new versions."
  (:require [iwaswhere-web.files :as f]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [clj-uuid :as uuid]))

(defn add-tags-mentions
  "Parses entry for hashtags and mentions."
  [entry]
  (when-let [text (:md entry)]
    (let [tags (set (re-seq #"(?m)(?!^)#[\w-]+" text))
          mentions (set (re-seq #"@\w+" text))]
      (merge entry
             {:tags     tags
              :mentions mentions}))))

(defn migrate-entries
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state."
  [conversion-fn]
  (let [files (file-seq (clojure.java.io/file "./data"))]
    (doseq [f (f/filter-by-name files #"\d{13}.edn")]
      (let [parsed (clojure.edn/read-string (slurp f))
            converted (conversion-fn parsed)
            filename (str "./data/" (:timestamp converted) ".edn")]
        (when converted
          (spit filename (with-out-str (pp/pprint converted))))))))


(defn migrate-to-uuids
  ; (migrate-to-uuids "./data/migration/daily-logs" "./data/daily-logs/2017-03-19.jrn")
  [path out-file]
  (let [files (file-seq (clojure.java.io/file path))
        ts-uuid (atom {})]
    (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}a?.jrn")]
      (with-open [reader (clojure.java.io/reader f)]
        (prn f)
        (let [lines (line-seq reader)]
          (doseq [line lines]
            (try
              (let [parsed (clojure.edn/read-string line)
                    ts (:timestamp parsed)
                    id (or (:id parsed)
                           (get-in @ts-uuid [ts])
                           (uuid/v1))
                    entry (merge parsed {:id id})
                    without-raw-exif (dissoc entry :raw-exif)
                    serialized (str (pr-str without-raw-exif) "\n")]
                (swap! ts-uuid assoc-in [ts] id)
                (spit out-file serialized :append true))
              (catch Exception ex
                (log/error "Exception" ex "when parsing line:\n" line)))))))
    (log/info (count @ts-uuid) "migrated")))

(defn migrate-books-to-sagas
  ; (migrate-to-uuids "./data/migration/book-to-saga" "./data/migration/2017-03-20.jrn")
  [path out-file]
  (let [files (file-seq (clojure.java.io/file path))
        ts-uuid (atom {})]
    (doseq [f (f/filter-by-name files #"\d{4}-\d{2}-\d{2}a?.jrn")]
      (with-open [reader (clojure.java.io/reader f)]
        (prn f)
        (let [lines (line-seq reader)]
          (doseq [line lines]
            (try
              (let [parsed (clojure.edn/read-string line)
                    entry (cond
                            (= (:entry-type parsed) :book)
                            (-> parsed
                                (assoc-in [:entry-type] :saga)
                                (assoc-in [:saga-name] (:book-name parsed))
                                (dissoc :book-name))

                            (= (:entry-type parsed) :story)
                            (-> parsed
                                (dissoc :linked-book)
                                (assoc-in [:linked-saga] (:linked-book parsed)))

                            :else parsed)
                    ts (:timestamp parsed)
                    serialized (str (pr-str entry) "\n")]
                (swap! ts-uuid assoc-in [ts] entry)
                (spit out-file serialized :append true))
              (catch Exception ex
                (log/error "Exception" ex "when parsing line:\n" line)))))))
    (log/info (count @ts-uuid) "migrated")))

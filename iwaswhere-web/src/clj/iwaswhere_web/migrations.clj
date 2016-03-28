(ns iwaswhere-web.migrations
  "This namespace is used for migrating entries to new versions."
  (:require [iwaswhere-web.store :as s]
            [clojure.pprint :as pp]))

(defn add-tags-mentions
  "Parses entry for hastags and mentions."
  [entry]
  (when-let [text (:md entry)]
    (let [
          tags (into [] (re-seq #"(?m)(?!^)#[\w-]+" text))
          mentions (into [] (re-seq #"@\\w+" text))]
      (merge entry
             {:tags     tags
              :mentions mentions}))))

(defn migrate-entries
  "Initial state function, creates state atom and then parses all files in
  data directory into the component state."
  [conversion-fn]
  (let [files (file-seq (clojure.java.io/file "./data"))]
    (doseq [f (s/filter-by-name files #"\d{13}.edn")]
      (let [parsed (clojure.edn/read-string (slurp f))
            converted (conversion-fn parsed)
            filename (str "./data/" (:timestamp converted) ".edn")]
        (when converted
          (spit filename (with-out-str (pp/pprint converted))))))))

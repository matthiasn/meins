(ns iwaswhere-web.fulltext-search
  "Lucene-based full-text index. In memory and reconstructed on application startup."
  (:require [clucy.core :as clucy]))

(defonce index (clucy/memory-index))

(defn remove-from-index
  "Remove entry from index."
  [index ts]
  (clucy/search-and-delete index (str ts)))

(defn add-to-index
  "Adds entry to Lucene index. Removes older version of the same entry first."
  [index entry]
  (remove-from-index index (:timestamp entry))
  (clucy/add index (select-keys entry [:timestamp :md])))

(defn search
  ""
  [query]
  (map #(Long. (:timestamp %)) (clucy/search index (:ft-search query) 1000)))

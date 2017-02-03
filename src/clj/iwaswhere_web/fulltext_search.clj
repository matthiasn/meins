(ns iwaswhere-web.fulltext-search
  "Lucene-based full-text index. In memory and reconstructed on application
   startup."
  (:require [clucy.core :as clucy]))

(defonce index (clucy/memory-index))

(defn remove-from-index
  "Remove entry from index."
  [{:keys [msg-payload]}]
  (let [ts (:timestamp msg-payload)]
    (clucy/search-and-delete index (str ts))))

(defn add-to-index
  "Adds entry to Lucene index. Removes older version of the same entry first."
  [{:keys [msg-payload]}]
  (let [entry (select-keys msg-payload [:timestamp :md :story-name :book-name])]
    (remove-from-index {:msg-payload entry})
    (clucy/add index entry)))

(defn search
  "Finds timestamps of entries matching a query."
  [query]
  (map #(Long. (:timestamp %)) (clucy/search index (:ft-search query) 1000)))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:ft/add    add-to-index
                 :ft/remove remove-from-index}})
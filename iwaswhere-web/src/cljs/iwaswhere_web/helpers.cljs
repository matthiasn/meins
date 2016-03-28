(ns iwaswhere-web.helpers
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [cljsjs.moment]))

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags in the new entry are contained in
  the filtered entry. This filters entries so that only entries that are relevant to the new
  entry are shown."
  ; TODO: also enable OR filter
  [new-entry]
  (fn [entry]
    (let [entry-tags (set (map s/lower-case (:tags entry)))
          new-entry-tags (set (map s/lower-case (:tags new-entry)))]
      ;      (set/subset? new-entry-tags entry-tags)
      (or (empty? new-entry-tags)
          (seq (set/intersection new-entry-tags entry-tags))))))

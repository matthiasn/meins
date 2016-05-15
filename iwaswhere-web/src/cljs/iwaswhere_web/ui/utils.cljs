(ns iwaswhere-web.ui.utils
  (:require [clojure.string :as s]
            [goog.dom.Range]
            [clojure.set :as set]))

(defn focus-on-end
  "Focus on the provided element, and then places the caret in the last position of the element's contents"
  [el]
  (.focus el)
  (doto (.createFromNodeContents goog.dom.Range el)
    (.collapse false)
    .select))

(defn pvt-filter
  "Filter for entries that I consider private."
  [entry]
  (let [tags (set (map s/lower-case (:tags entry)))
        private-tags #{"#pvt" "#private" "#nsfw"}
        matched (set/intersection tags private-tags)]
    (empty? matched)))
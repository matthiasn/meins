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

(defn btn-w-tooltip
  "Render button with tooltip on top."
  [icon-cls text tooltip-text click-fn btn-cls]
  [:span.tooltip
   [:button.pure-button.button-xsmall.tooltip {:on-click click-fn :class btn-cls}
    [:span.fa {:class icon-cls}] (str " " text)]
   [:span.tooltiptext tooltip-text]])

(defn span-w-tooltip
  "Render button with tooltip on top."
  [icon-cls tooltip-text click-fn]
  [:span.fa.toggle.tooltip {:on-click click-fn :class icon-cls}
   [:span.tooltiptext tooltip-text]])

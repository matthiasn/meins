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

(defn suggestions
  "Renders suggestions for hashtags or mentions if either occurs before the current caret position.
  It does so by getting the selection from the DOM API, which can be used to determine the position
  and a string before that position, then finding either a hashtag or mention fragment right at the
  and of that substring. For these, auto-suggestions are displayed, which are entities that begin
  with the tag fragment before the caret position. When any of the suggestions are clicked, the
  fragment will be replaced with the clicked item."
  [key-prefix filtered-tags current-tag tag-replace-fn css-class]
  (when (seq filtered-tags)
    [:div.suggestions
     [:div.suggestions-list
      (for [tag filtered-tags]
        ^{:key (str key-prefix tag)}
        [:div {:on-click #(tag-replace-fn current-tag tag)}
         [:span {:class css-class} tag]])]]))

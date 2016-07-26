(ns iwaswhere-web.ui.utils
  (:require [clojure.string :as s]
            [clojure.set :as set]))

(defn duration-string
  "Format duration string from seconds."
  [seconds]
  (let [hours (int (/ seconds 3600))
        seconds (rem seconds 3600)
        min (int (/ seconds 60))
        sec (rem seconds 60)]
    (s/trim
      (str (when (pos? hours) (str hours "h "))
           (when (pos? min) (str min "m "))
           (when (and (not (pos? hours)) (pos? sec)) (str sec "s"))))))

(defn visit-duration
  "Formats duration string."
  [entry]
  (let [arrival-ts (:arrival-timestamp entry)
        depart-ts (:departure-timestamp entry)
        dur (when (and arrival-ts depart-ts)
              (let [dur (- depart-ts arrival-ts)]
                (if (int? dur)
                  (/ dur 1000)
                  (Math/floor dur))))]
    (when (and dur (< dur 99999999))
      (str ", " (duration-string dur)))))

(defn pvt-filter
  "Filter for entries that I consider private."
  [entry]
  (let [tags (set (map s/lower-case (:tags entry)))
        private-tags #{"#pvt" "#private" "#nsfw"}
        matched (set/intersection tags private-tags)]
    (empty? matched)))

(defn suggestions
  "Renders suggestions for hashtags or mentions if either occurs before the
   current caret position. It does so by getting the selection from the DOM API,
   which can be used to determine the position and a string before that
   position, then finding either a hashtag or mention fragment right at the end
   of that substring. For these, auto-suggestions are displayed, which are
   entities that begin with the tag fragment before the caret position. When any
   of the suggestions are clicked, the fragment will be replaced with the
   clicked item."
  [key-prefix filtered-tags current-tag tag-replace-fn css-class]
  (when (seq filtered-tags)
    [:div.suggestions
     [:div.suggestions-list
      (for [tag filtered-tags]
        ^{:key (str key-prefix tag)}
        [:div {:on-click #(tag-replace-fn current-tag tag)}
         [:span {:class css-class} tag]])]]))

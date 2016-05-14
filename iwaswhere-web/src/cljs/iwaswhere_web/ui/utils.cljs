(ns iwaswhere-web.ui.utils
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [goog.dom.Range]))

(defn hashtags-replacer
  "Replaces hashtags in entry text. Depending on show-hashtags? switch either displays
  the hashtag or not. Creates link for each hashtag, which opens iWasWhere in new tab,
  with the filter set to the clicked hashtag."
  [show-hashtags?]
  (fn [acc hashtag]
    (let [f-hashtag (if show-hashtags? hashtag (subs hashtag 1))
          with-link (str " <a target='_blank' href='/#" hashtag "'>" f-hashtag "</a>")]
      (s/replace acc (re-pattern (str "[^*]" hashtag "(?!\\w)")) with-link))))

(defn mentions-replacer
  "Replaces mentions in entry text."
  [acc mention]
  (let [with-link (str " <a class='mention-link' target='_blank' href='/#" mention "'>" mention "</a>")]
    (s/replace acc mention with-link)))

(defn- reducer
  "Generic reducer, allows calling specified function for each item in the collection."
  [text coll fun]
  (reduce fun text coll))

(def initial-focus-wrapper
  "Wrapper that automatically focuses on the provided element, and then places the caret in the last
  position of the element's contents.
  Inspired by:
   - http://stackoverflow.com/questions/27602592/reagent-component-did-mount
   - http://stackoverflow.com/questions/4233265/contenteditable-set-caret-at-the-end-of-the-text-cross-browser"
  (with-meta identity
             {:component-did-mount #(let [el (r/dom-node %)]
                                     (.focus el)
                                     (doto (.createFromNodeContents goog.dom.Range el)
                                       (.collapse false)
                                       .select))}))

(ns iwaswhere-web.markdown
  (:require [markdown.core :as md]
            [clojure.string :as s]
            [cljsjs.moment]))

(defn hashtags-replacer
  "Replaces hashtags in entry text."
  [acc hashtag]
  (s/replace acc hashtag (str "**" hashtag "**")))

(defn mentions-replacer
  "Replaces mentions in entry text."
  [acc mention]
  (s/replace acc mention (str "**_" mention "_**")))

(defn- reducer
  "Generic reducer, allows calling specified function for each item in the collection."
  [text coll fun]
  (reduce fun text coll))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous here since
  application is only running locally, so in doubt we could only harm ourselves."
  [entry]
  (let [md-string (-> entry
                      :md
                      (reducer (:tags entry) hashtags-replacer)
                      (reducer (:mentions entry) mentions-replacer))]
    [:div {:dangerouslySetInnerHTML {:__html (md/md->html md-string)}}]))

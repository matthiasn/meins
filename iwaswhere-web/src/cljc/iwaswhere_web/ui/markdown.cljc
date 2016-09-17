(ns iwaswhere-web.ui.markdown
  "This namespace holds the functions for rendering the text (markdown) content
   of a journal entry.
   This includes both a properly styled element for static content and the
   edit-mode view, with autosuggestions for tags and mentions."
  (:require [markdown.core :as md]
            [clojure.string :as s]
    #?(:cljs [reagent.core :as rc])))

(def tag-char-class "[\\w\\-\\u00C0-\\u017F]")

(defn hashtags-replacer
  "Replaces hashtags in entry text. Depending on show-hashtags? switch either
   displays the hashtag or not. Creates link for each hashtag, which opens
   iWasWhere in new tab, with the filter set to the clicked hashtag."
  [show-hashtags?]
  (fn [acc hashtag]
    (let [f-hashtag (if show-hashtags? hashtag (subs hashtag 1))
          with-link (str " <a href='/#" hashtag "'>" f-hashtag "</a>")]
      (s/replace acc (re-pattern (str "[^*]" hashtag
                                      "(?!" tag-char-class ")(?![`)])"))
                 with-link))))

(defn mentions-replacer
  "Replaces mentions in entry text."
  [show-hashtags?]
  (fn [acc mention]
    (let [f-mention (if show-hashtags? mention (subs mention 1))
          with-link (str " <a class='mention-link' href='/#" mention
                         "'>" f-mention "</a>")]
      (s/replace acc (re-pattern (str mention "(?!" tag-char-class ")"))
                 with-link))))

(defn reducer
  "Generic reducer, allows calling specified function for each item in the
   collection."
  [text coll fun]
  (reduce fun text coll))

(defn mk-format-tags-xform
  "Make custom transformer for hashtags and mentions."
  [entry show-hashtags?]
  (fn [text state]
    [(if (:codeblock state)
       text
       (-> text
           (reducer (:tags entry) (hashtags-replacer show-hashtags?))
           (reducer (:mentions entry) (mentions-replacer show-hashtags?))))
     state]))

(def md-to-html #?(:clj md/md-to-html-string :cljs md/md->html))
(def initial {:show-shortened   true
              :recently-clicked false})
(def initial-atom #?(:clj (atom initial) :cljs (rc/atom initial)))

(defn markdown-render
  "Renders a markdown div using :dangerouslySetInnerHTML. Not that dangerous
   here since application is only running locally, so in doubt we could only
   harm ourselves. Returns nil when entry does not contain markdown text."
  [entry cfg toggle-edit]
  (fn [entry cfg toggle-edit]
    (when-let [md-string (:md entry)]
      (let [show-hashtags? (not (:hide-hashtags cfg))
            lines-shortened (:lines-shortened cfg)
            lines (s/split-lines md-string)
            shortened? (and (:show-shortened @initial-atom)
                            (>= (count lines) lines-shortened))
            md-string (if shortened?
                        (let [lines (take lines-shortened lines)]
                          (s/join "\n" lines))
                        md-string)
            tags-xform (mk-format-tags-xform entry show-hashtags?)
            html (md-to-html md-string :custom-transformers [tags-xform])

            on-click-fn
            (fn [_ev]
              (when (:recently-clicked @initial-atom)
                (toggle-edit))
              (swap! initial-atom update-in [:recently-clicked] not)
              #?(:cljs
                 (.setTimeout
                   js/window
                   (fn []
                     (swap! initial-atom assoc-in [:recently-clicked] false))
                   500)))]
        [:div {:on-click on-click-fn}
         (when (:redacted cfg) {:class "redacted"})
         [:div {:dangerouslySetInnerHTML {:__html html}}]
         (when shortened?
           [:span.more
            {:on-mouse-enter #(swap! initial-atom update-in [:show-shortened] not)}
            "[...]"])]))))

(defn count-words
  "Naive implementation of a wordcount function."
  [entry]
  (if-let [text (:md entry)]
    (count (s/split text #" "))
    0))

(defn count-words-formatted
  "Generate wordcount string."
  [entry]
  (let [cnt (count-words entry)]
    (when (> cnt 20)
      (str cnt " words"))))

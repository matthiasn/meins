(ns iwaswhere-web.utils.parse
  "Parsing functions, tested in 'iwaswhere-web.parse-test' namespace."
  (:require [clojure.string :as s]))

(def tag-char-class "[\\w\\-\\u00C0-\\u017F]")

(def search-tag-regex      (re-pattern (str "(?m)(?:^|[^~])(#" tag-char-class "+)")))
(def search-not-tags-regex (re-pattern (str "(?m)~#" tag-char-class "+")))
(def search-mention-regex  (re-pattern (str "(?m)@" tag-char-class "+")))
(def entry-tag-regex       (re-pattern (str "(?m)(?!^) ?#" tag-char-class "+(?!" tag-char-class ")(?![`)])")))
(def entry-mentions-regex  (re-pattern (str "(?m) ?@" tag-char-class "+(?!" tag-char-class ")(?![`)])")))

(defn parse-entry
  "Parses entry for hashtags and mentions. Either can consist of any of the word characters, dashes
  and unicode characters that for example comprise German 'Umlaute'.
  The negative lookahead (?!`) makes sure that tags and mentions are not matched and processed
  when they are quoted as code with backticks."
  [text]
  {:md       text
   :tags     (set (map s/trim (re-seq entry-tag-regex text)))
   :mentions (set (map s/trim (re-seq entry-mentions-regex text)))})

(defn parse-search
  "Parses search string for hashtags, mentions, and hashtags that should not be contained in the filtered entries.
  Such hashtags can for now be marked like this: #~done. Finding tasks that are not done, which don't have #done
  in either the entry or any of its comments, can be found like this: #task #~done"
  [text]
  {:search-text text
   :tags        (set (map second (re-seq search-tag-regex text)))
   :not-tags    (set (re-seq search-not-tags-regex text))
   :mentions    (set (re-seq search-mention-regex text))
   :date-string (re-find #"[0-9]{4}-[0-9]{2}-[0-9]{2}" text)
   :timestamp   (re-find #"[0-9]{13}" text)
   :n           40})

(defn autocomplete-tags
  "Determine autocomplete options for the partial tag (or mention) before the cursor."
  [before-cursor regex-prefix tags]
  (let [current-tag (s/trim (str (re-find (re-pattern (str "(?i)" regex-prefix tag-char-class "+$")) before-cursor)))
        current-tag-regex (re-pattern (str "(?i)" current-tag))
        tag-substr-filter (fn [tag] (when (seq current-tag) (re-find current-tag-regex tag)))
        f-tags (set (filter tag-substr-filter tags))]
    [current-tag f-tags]))

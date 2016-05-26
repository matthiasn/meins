(ns iwaswhere-web.helpers
  (:require [matthiasn.systems-toolbox.component :as st]))

(defn send-w-geolocation
  "Calls geolocation, sends entry enriched by geo information inside the
  callback function"
  [data put-fn]
  (.getCurrentPosition
    (.-geolocation js/navigator)
    (fn [pos]
      (let [coords (.-coords pos)]
        (put-fn [:entry/geo-enrich
                 (merge data {:latitude  (.-latitude coords)
                              :longitude (.-longitude coords)})])))))

(def tag-char-class "[\\w\\-\\u00C0-\\u017F]")

(defn parse-entry
  "Parses entry for hashtags and mentions. Either can consist of any of the word characters, dashes
  and unicode characters that for example comprise German 'Umlaute'.
  The negative lookahead (?!`) makes sure that tags and mentions are not found and processed
  when they are quoted as code with backticks."
  [text]
  (let [tags (set (re-seq (js/RegExp. (str "(?!^)#" tag-char-class "+(?!" tag-char-class ")(?![`)])") "m") text))
        mentions (set (re-seq (js/RegExp. (str "@" tag-char-class "+(?!" tag-char-class ")(?![`)])") "m") text))]
    {:md        text
     :tags      tags
     :mentions  mentions}))

(defn new-entry-fn
  "Create a new, empty entry. The opts map is merged last with the generated entry, thus keys can
  be overwritten here.
  Caveat: the timezone detection currently only works in Chrome. My Firefox 46.0.1 strictly refused
  to tell me a timezone when calling 'Intl.DateTimeFormat().resolvedOptions()', which would be
  according to standards but unfortunately the timeZone is always undefined."
  [put-fn opts]
  (fn [_ev]
    (let [ts (st/now)
          entry (merge (parse-entry "")
                       {:timestamp  ts
                        :new-entry  true
                        :timezone   (or (when-let [resolved (.-resolved (new js/Intl.DateTimeFormat))]
                                          (.-timeZone resolved))
                                        (when-let [resolved (.resolvedOptions (new js/Intl.DateTimeFormat))]
                                          (.-timeZone resolved)))
                        :utc-offset (.getTimezoneOffset (new js/Date))}
                       opts)]
      (put-fn [:entry/new entry])
      (send-w-geolocation entry put-fn))))

(defn parse-search
  "Parses search string for hashtags, mentions, and hashtags that should not be contained in the filtered entries.
  Such hashtags can for now be marked like this: #~done. Finding tasks that are not done, which don't have #done
  in either the entry or any of its comments, can be found like this: #task #~done"
  [text]
  {:search-text text
   :tags        (set (map second (re-seq (js/RegExp. (str "(?:^|[^~])(#" tag-char-class "+)") "m") text)))
   :not-tags    (set (re-seq (js/RegExp. (str "~#" tag-char-class "+") "m") text))
   :mentions    (set (re-seq (js/RegExp. (str "@" tag-char-class "+") "m") text))
   :date-string (re-find #"[0-9]{4}-[0-9]{2}-[0-9]{2}" text)
   :timestamp   (re-find #"[0-9]{13}" text)
   :n           40})

(defn clean-entry
  [entry]
  (-> entry
      (dissoc :comments)
      (dissoc :new-entry)
      (dissoc :linked-entries-list)))

(defn query-from-search-hash
  "Get query from location hash for current page."
  []
  (let [search-hash (subs (js/decodeURIComponent (aget js/window "location" "hash")) 1)]
    (parse-search search-hash)))

(defn autocomplete-tags
  "Determine autocomplete options for the partial tag (or mention) before the cursor."
  [before-cursor regex-prefix tags]
  (let [current-tag (re-find (js/RegExp. (str regex-prefix tag-char-class "+$") "") before-cursor)
        current-tag-regex (js/RegExp. current-tag "i")
        tag-substr-filter (fn [tag] (when current-tag (re-find current-tag-regex tag)))
        f-tags (filter tag-substr-filter tags)]
    [current-tag f-tags]))

(defn string-before-cursor
  "Determine the substring right before the cursor of the current selection. Only returns that
  substring if it is from current node's text, as otherwise this would listen to selections
  outside the element as well."
  [comp-str]
  (let [selection (.getSelection js/window)
        cursor-pos (.-anchorOffset selection)
        anchor-node (aget selection "anchorNode")
        node-value (str (when anchor-node (aget anchor-node "nodeValue")) "")]
    (if (not= -1 (.indexOf (str comp-str) node-value))
      (subs node-value 0 cursor-pos)
      "")))

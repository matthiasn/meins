(ns iwaswhere-web.helpers
  (:require [cljsjs.moment]
            [matthiasn.systems-toolbox.component :as st]))

(defn send-w-geolocation
  "Calls geolocation, sends entry enriched by geo information inside the
  callback function"
  [data put-fn]
  (.getCurrentPosition
    (.-geolocation js/navigator)
    (fn [pos]
      (let [coords (.-coords pos)]
        (put-fn [:geo-entry/persist
                 (merge data {:latitude  (.-latitude coords)
                              :longitude (.-longitude coords)})])))))

(def tag-char-class "[\\w\\-\\u00C0-\\u017F]")

(defn parse-entry
  "Parses entry for hashtags and mentions. Either can consist of any of the word characters, dashes
  and unicode characters that for example comprise German 'Umlaute'."
  [text]
  (let [tags (set (re-seq (js/RegExp. (str "(?!^)#" tag-char-class "+(?!" tag-char-class ")") "m") text))
        mentions (set (re-seq (js/RegExp. (str "@" tag-char-class "+(?!" tag-char-class ")") "m") text))]
    {:md        text
     :tags      tags
     :mentions  mentions}))

(defn new-entry-fn
  "Create a new, empty entry. The opts map is merged last with the generated entry, thus keys can
  be overwritten here."
  [put-fn opts]
  (fn [_ev]
    (let [ts (st/now)
          entry (merge (parse-entry "") {:timestamp ts :tags #{"#new-entry"}} opts)]
      (put-fn [:geo-entry/persist entry])
      (send-w-geolocation entry put-fn))))

(defn parse-search
  "Parses search string for hashtags, mentions, and hashtags that should not be contained in the filtered entries.
  Such hashtags can for now be marked like this: #~done. Finding tasks that are not done, which don't have #done
  in either the entry or any of its comments, can be found like this: #task #~done"
  [text]
  {:search-text text
   :tags        (set (re-seq (js/RegExp. (str "#" tag-char-class "+") "m") text))
   :not-tags    (set (re-seq (js/RegExp. (str "#~" tag-char-class "+") "m") text))
   :mentions    (set (re-seq (js/RegExp. (str "@" tag-char-class "+") "m") text))
   :date-string (re-find #"[0-9]{4}-[0-9]{2}-[0-9]{2}" text)
   :timestamp   (re-find #"[0-9]{13}" text)})

(defn query-from-search-hash
  "Get query from location hash for current page."
  []
  (let [search-hash (subs (js/decodeURIComponent (aget js/window "location" "hash")) 1)]
    (parse-search search-hash)))
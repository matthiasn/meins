(ns iwaswhere-web.helpers
  (:require [clojure.string :as s]
            [clojure.set :as set]
            [cljsjs.moment]
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

(defn entries-filter-fn
  "Creates a filter function which ensures that all tags in the new entry are contained in
  the filtered entry. This filters entries so that only entries that are relevant to the new
  entry are shown."
  ; TODO: also enable OR filter
  [new-entry]
  (fn [entry]
    (let [entry-tags (set (map s/lower-case (:tags entry)))
          new-entry-tags (set (map s/lower-case (:tags new-entry)))]
      ;      (set/subset? new-entry-tags entry-tags)
      (or (empty? new-entry-tags)
          (seq (set/intersection new-entry-tags entry-tags))))))

(defn parse-entry
  "Parses entry for hashtags and mentions. Either can consist of any of the word characters, dashes
  and unicode characters that for example comprise German 'Umlaute'."
  [text]
  (let [tags (set (re-seq (js/RegExp. "(?!^)#[\\w\\-\\u00C0-\\u017F]+" "m") text))
        mentions (set (re-seq (js/RegExp. "@[\\w\\-\\u00C0-\\u017F]+" "m") text))]
    {:md        text
     :tags      tags
     :mentions  mentions}))

(defn new-entry-fn
  "Create a new, empty entry. The opts map is merged last with the generated entry, thus keys can
  be overwritten here."
  [put-fn opts]
  (fn [_ev]
    (let [ts (st/now)
          entry (merge (parse-entry "...") {:timestamp ts} opts)]
      (put-fn [:geo-entry/persist entry])
      (put-fn [:cmd/toggle {:timestamp ts :key :show-edit-for}])
      (send-w-geolocation entry put-fn))))

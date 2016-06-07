(ns iwaswhere-web.helpers
  (:require [matthiasn.systems-toolbox.component :as st]
            [goog.dom.Range]
            [clojure.string :as s]
            [iwaswhere-web.utils.parse :as p]))

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

(defn new-entry-fn
  "Create a new, empty entry. The opts map is merged last with the generated entry, thus keys can
  be overwritten here.
  Caveat: the timezone detection currently only works in Chrome. My Firefox 46.0.1 strictly refused
  to tell me a timezone when calling 'Intl.DateTimeFormat().resolvedOptions()', which would be
  according to standards but unfortunately the timeZone is always undefined."
  [put-fn opts]
  (fn [_ev]
    (let [ts (st/now)
          entry (merge (p/parse-entry "")
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

(defn clean-entry
  [entry]
  (-> entry
      (dissoc :comments)
      (dissoc :new-entry)
      (dissoc :pomodoro-running)
      (dissoc :linked-entries-list)))

(defn query-from-search-hash
  "Get query from location hash for current page."
  []
  (let [search-hash (subs (js/decodeURIComponent (aget js/window "location" "hash")) 1)]
    (p/parse-search search-hash)))

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

(defn focus-on-end
  "Focus on the provided element, and then places the caret in the last position of the element's contents"
  [el]
  (.focus el)
  (doto (.createFromNodeContents goog.dom.Range el)
    (.collapse false)
    .select))

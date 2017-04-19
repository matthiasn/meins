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
  "Create a new, empty entry. The opts map is merged last with the generated
   entry, thus keys can be overwritten here.
   Caveat: the timezone detection currently only works in Chrome. TODO: check
   "
  [put-fn opts run-fn]
  (fn [_ev]
    (let [ts (st/now)
          timezone (or (when-let [resolved (.-resolved (new js/Intl.DateTimeFormat))]
                         (.-timeZone resolved))
                       (when-let [resolved (.resolvedOptions (new js/Intl.DateTimeFormat))]
                         (.-timeZone resolved)))
          entry (merge (p/parse-entry "")
                       {:timestamp  ts
                        :new-entry  true
                        :timezone   timezone
                        :utc-offset (.getTimezoneOffset (new js/Date))}
                       opts)]
      (put-fn [:entry/new entry])
      (send-w-geolocation entry put-fn)
      (when run-fn (run-fn)))))

(defn string-before-cursor
  "Determine the substring right before the cursor of the current selection.
   Only returns that substring if it is from current node's text, as otherwise
   this would listen to selections outside the element as well."
  [comp-str]
  (let [selection (.getSelection js/window)
        cursor-pos (.-anchorOffset selection)
        anchor-node (aget selection "anchorNode")
        node-value (str (when anchor-node (aget anchor-node "nodeValue")) "")]
    (if (and (not= -1 (.indexOf (str comp-str) node-value))
             (pos? cursor-pos))
      (subs node-value 0 cursor-pos)
      "")))

(defn focus-on-end
  "Focus on the provided element, and then places the caret in the last position
   of the element's contents."
  [el]
  (.focus el)
  (doto (.createFromNodeContents goog.dom.Range el)
    (.collapse false)
    .select))

(defn prevent-default [ev] (.preventDefault ev))

(defn add [x y] (+ (or x 0) (or y 0)))

(defn update-numeric [entry path put-fn]
  (fn [ev]
    (let [v (.. ev -target -value)
          parsed (when (seq v) (js/parseFloat v))
          updated (assoc-in entry path parsed)]
      (when parsed
        (put-fn [:entry/update-local updated])))))

(def ymd-format "YYYY-MM-DD")
(defn n-days-ago [n] (.subtract (js/moment.) n "d"))
(defn n-days-ago-fmt [n] (.format (n-days-ago n) ymd-format))
(defn format-time [m] (.format (js/moment m) "YYYY-MM-DDTHH:mm"))

(defn get-stats
  "Retrieves stats for the last n days."
  [stats-key put-fn n]
  (let [days (map n-days-ago-fmt (reverse (range n)))]
    (put-fn [:stats/get {:days (mapv (fn [d] {:date-string d}) days)
                         :type stats-key}])))

(defn keep-updated
  [stats-key n local last-update put-fn]
  (when (not= last-update (:last-fetched @local))
    (get-stats stats-key put-fn n)
    (prn :update stats-key)
    (swap! local assoc-in [:last-fetched] last-update)))

(defn print-duration
  "Helper for inspecting where time is spent."
  [msg-meta]
  (when msg-meta
    (let [store-m (:client/store-cmp msg-meta)
          store-duration (- (:in-ts store-m) (:out-ts store-m))
          cli-ws-m (:client/ws-cmp msg-meta)
          cli-ws-dur (- (:out-ts cli-ws-m) (:in-ts cli-ws-m))
          srv-ws-m (:server/ws-cmp msg-meta)
          srv-ws-dur (- (:in-ts srv-ws-m) (:out-ts srv-ws-m))
          srv-store-m (:server/store-cmp msg-meta)
          srv-store-dur (- (:out-ts srv-store-m) (:in-ts srv-store-m))]
      (prn msg-meta)
      (prn "duration from and to :client/store-cmp in ms:" store-duration)
      (prn "duration from and to :client/ws-cmp in ms:" cli-ws-dur)
      (prn "ms from :client/store-cmp to :client/ws-cmp:" (- (:in-ts cli-ws-m) (:out-ts store-m)))
      (prn "ms from :client/ws-cmp to :client/store-cmp:" (- (:in-ts store-m) (:out-ts cli-ws-m)))
      (prn "ms from :client/ws-cmp to :server/ws-cmp:" (- (:out-ts srv-ws-m) (:in-ts cli-ws-m)))
      (prn "ms from :server/ws-cmp to :client/ws-cmp:" (- (:out-ts cli-ws-m) (:in-ts srv-ws-m)))
      (prn "duration from and to :server/ws-cmp in ms:" srv-ws-dur)
      (prn "duration from and to :server/store-cmp in ms:" srv-store-dur))))

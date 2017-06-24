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
                              :longitude (.-longitude coords)})])))
    (fn [err] (prn err))))

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
  [stats-key n m put-fn]
  (let [days (map n-days-ago-fmt (reverse (range n)))]
    (put-fn (with-meta
              [:stats/get {:days (mapv (fn [d] {:date-string d}) days)
                           :type stats-key}]
              m))))

(defn keep-updated
  [stats-key n local last-update put-fn]
  (when (not= (:last-update last-update) (:last-fetched @local))
    (swap! local assoc-in [:last-fetched] (:last-update last-update))
    (get-stats stats-key n (:meta last-update {}) put-fn)))

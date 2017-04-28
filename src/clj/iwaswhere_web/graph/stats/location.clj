(ns iwaswhere-web.graph.stats.location
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [iwaswhere-web.graph.query :as gq]
            [clj-time.core :as t]
            [iwaswhere-web.utils.misc :as u]
            [clj-time.format :as ctf]
            [clojure.tools.logging :as log]
            [ubergraph.core :as uc]
            [clojure.pprint :as pp]
            [clj-time.coerce :as ctc]
            [clj-time.core :as ct]))

(defn locations
  "Gathers information about places visited."
  [current-state]
  (time
    (let [q {:mentions    #{}
             :tags        #{}
             :n           30000
             :not-tags    #{}
             :search-text ""}
          res (gq/get-filtered current-state q)
          entries (vals (:entries-map res))
          local-fmt (ctf/with-zone (ctf/formatters :year-month-day)
                                   (ct/default-time-zone))
          by-country-fn
          (fn [acc entry]
            (let []
              (if-let [geoname (:geoname entry)]
                (let [country (:country-code geoname)
                      ts (:timestamp entry)
                      day (ctf/unparse local-fmt (ctc/from-long ts))]
                  (-> acc
                      (update-in [:country-days country] #(set (conj % day)))
                      (update-in [:days-countries day] #(set (conj % country)))
                      (update-in [:country-entries country] #(inc (or % 0)))))
                acc)))
          acc (reduce by-country-fn {} entries)
          days-per-country (into {} (map (fn [[c days]] [c (count days)])
                                         (:country-days acc)))
          locations-stats (merge acc {:days-per-country days-per-country})]
      (log/info (count entries))
      (log/info days-per-country)
      (log/info (:country-entries acc))
      locations-stats)))


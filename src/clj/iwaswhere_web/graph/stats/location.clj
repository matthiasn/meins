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
                (let [gname (:name geoname)
                      admin-1-name (:admin-1-name geoname)
                      admin-2-name (:admin-2-name geoname)
                      admin-3-name (:admin-3-name geoname)
                      admin-4-name (:admin-4-name geoname)
                      country (:country-code geoname)
                      ts (:timestamp entry)
                      day (ctf/unparse local-fmt (ctc/from-long ts))]
                  (-> acc
                      (update-in [:country-days country] #(set (conj % day)))
                      (update-in [:days-countries day] #(set (conj % country)))
                      (update-in [:country-entries country] #(inc (or % 0)))
                      (update-in [:location-days gname] #(set (conj % day)))
                      (update-in [:admin-1-days admin-1-name] #(set (conj % day)))
                      (update-in [:admin-2-days admin-2-name] #(set (conj % day)))
                      (update-in [:admin-3-days admin-3-name] #(set (conj % day)))
                      (update-in [:admin-4-days admin-4-name] #(set (conj % day)))))
                acc)))
          acc (reduce by-country-fn {} entries)
          count-days (fn [[c days]] [c (count days)])
          locations-stats
          {:days-per-country  (into {} (map count-days (:country-days acc)))
           :days-per-location (into {} (map count-days (:location-days acc)))
           :days-per-admin-1  (into {} (map count-days (:admin-1-days acc)))
           :days-per-admin-2  (into {} (map count-days (:admin-2-days acc)))
           :days-per-admin-3  (into {} (map count-days (:admin-3-days acc)))
           :days-per-admin-4  (into {} (map count-days (:admin-4-days acc)))}]
      (log/info (count entries))
      (log/info locations-stats)
      (log/info (:country-entries acc))
      locations-stats)))


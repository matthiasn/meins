(ns iww.electron.geonames.geonames
  (:require [taoensso.timbre :as timbre :refer-macros [info error]]
            [electron :refer [app]]
            [fs :refer [mkdirSync existsSync]]
            [local-reverse-geocoder :as geocoder]
            [cljs.nodejs :as nodejs :refer [process]]))

(aset js/console "log" #(info "GEOCODER" %))

(defn lookup [{:keys [msg-payload put-fn]}]
  (let [points (clj->js msg-payload)
        callback (fn [err addresses]
                   (when err (error "GEOCODER" err))
                   (when addresses
                     (info "GEOCODER result" msg-payload (js->clj addresses))))
        res (.lookUp geocoder points 1 callback)])
  {})

(defn state-fn [put-fn]
  (let [state (atom {})
        dump-dir "/tmp/geonames1"]
    (info "Starting GEONAMES")
    (when-not (existsSync dump-dir)
      (mkdirSync dump-dir))
    (.init geocoder
           (clj->js {:dumpDirectory dump-dir
                     :load          {:admin1         true
                                     :admin2         true
                                     :admin3And4     false
                                     :alternateNames false}})
           #(do
              (info "GEOCODER started")
              (lookup {:msg-payload [{:latitude  53.5805329
                                      :longitude 9.964600599999999}]})))
    {:state state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:geonames/lookup lookup}})

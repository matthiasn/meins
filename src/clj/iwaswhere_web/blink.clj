(ns iwaswhere-web.blink
  "Component for interacting with the awesome blink1 USB RGB LED notification
   light: https://blink1.thingm.com. Here, this little device is used to
   display if I should be busy or not."
  (:require [matthiasn.systems-toolbox.component :as st]
            [clojure.java.shell :refer [sh]]
            [me.raynes.fs :as fs]
            [clj-time.core :as ct]
            [clj-time.local :as ctl]
            [clojure.tools.logging :as log]
            [iwaswhere-web.zipkin :as z]))

(def red {:day "--hsb=265,255,255" :night "--hsb=265,255,100"})
(def green {:day "--hsb=65,255,255" :night "--hsb=65,255,100"})

(defn blink
  "Calls the blink1 command line tool, which can for example be installed via
   homebrew with 'brew install blink1'. Does nothing when the binary doesn't
   exist."
  [args]
  (let [blink-path "./blink1-tool"]
    (if (fs/exists? blink-path)
      (sh blink-path args))))

(defn state-fn
  "Create initial state and send of scheduling request for :blink/heartbeat"
  [put-fn]
  (let [state (atom {:last-busy 0})]
    (put-fn [:cmd/schedule-new {:timeout 1000
                                :message [:blink/heartbeat]
                                :repeat  true}])
    {:state state}))

(defn day-night?
  []
  (let [h (ct/hour (ctl/local-now))]
    (if (or (< h 8) (> h 18)) :night :day)))

(defn blink-fn
  "Called with each heartbeat. Sets color to green when last busy timestamp is
   longer ago than one second."
  [{:keys [current-state]}]
  (let [now (st/now)
        k (day-night?)
        last-busy (get-in current-state [:last-busy])]
    (when (> (- now last-busy) 1000)
      (log/debug "blink green")
      (blink (k green)))
    {}))

(defn blink-busy
  "Set light to red when busy and save last busy timestamp."
  [{:keys [current-state]}]
  (let [ts (st/now)
        k (day-night?)]
    (log/debug "blink red")
    (blink (k red))
    {:new-state (assoc-in current-state [:last-busy] ts)}))

(defn cmp-map
  "Generates component map for blink-cmp."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:blink/heartbeat blink-fn
                 :blink/busy      blink-busy}})

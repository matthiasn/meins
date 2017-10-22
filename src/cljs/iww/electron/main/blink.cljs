(ns iww.electron.main.blink
  "Component for interacting with the awesome blink1 USB RGB LED notification
   light: https://blink1.thingm.com. Here, this little device is used to
   display if I should be busy or not."
  (:require [matthiasn.systems-toolbox.component :as st]
            [taoensso.timbre :as timbre :refer-macros [debug]]
            [iww.electron.main.runtime :as rt]
            [fs :refer [existsSync]]
            [child_process :refer [spawn]]
            [moment]))

(def red {:day "--hsb=10,255,255" :night "--hsb=10,255,100"})
(def yellow {:day "--hsb=32,255,255" :night "--hsb=32,255,100"})
(def green {:day "--hsb=65,255,255" :night "--hsb=65,255,100"})

(defn spawn-process [cmd args opts]
  (debug "BLINK: spawning" cmd args opts)
  (spawn cmd (clj->js args) (clj->js opts)))

(defn blink
  "Calls the blink1 command line tool, which can for example be installed via
   homebrew with 'brew install blink1'. Does nothing when the binary doesn't
   exist."
  [args]
  (let [blink-path (:blink rt/runtime-info)]
    (if (existsSync blink-path)
      (spawn-process blink-path [args] []))))

(defn state-fn [put-fn]
  (let [state (atom {:last-busy 0})]
    (put-fn [:cmd/schedule-new {:timeout 1000
                                :message [:blink/heartbeat]
                                :repeat  true}])
    {:state state}))

(defn day-night? []
  (let [h (.hour (moment))]
    (if (or (< h 8) (> h 18)) :night :day)))

(defn blink-fn
  "Sets color to green when last busy timestamp is longer ago than one second.
   Called with each heartbeat."
  [{:keys [current-state]}]
  (let [now (st/now)
        k (day-night?)
        last-busy (get-in current-state [:last-busy])]
    (when (> (- now last-busy) 1000)
      (debug "blink green")
      (blink (k green)))
    {}))

(defn blink-busy
  "Set light to red when busy and save last busy timestamp."
  [{:keys [current-state msg-payload]}]
  (let [ts (st/now)
        pomodoro-completed (:pomodoro-completed msg-payload)
        k (day-night?)]
    (debug "blink red")
    (blink (k (if pomodoro-completed yellow red)))
    {:new-state (assoc-in current-state [:last-busy] ts)}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:blink/heartbeat blink-fn
                 :blink/busy      blink-busy}})

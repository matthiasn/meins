(ns iwaswhere-web.datetime
  (:require [clj-time.core :as ct]
            [clj-time.format :as ctf]))

(def datetime-local-fmt
  (ctf/formatter "yyyy-MM-dd'T'HH:mm" (ct/default-time-zone)))
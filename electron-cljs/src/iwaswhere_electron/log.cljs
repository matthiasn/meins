(ns iwaswhere-electron.log
  (:require [electron-log :as l]
            [cljs.nodejs :as nodejs]))

(aset l "transports" "console" "level" "info")
(aset l "transports" "console" "format" "{h}:{i}:{s}:{ms} {text}")
(aset l "transports" "file" "level" "info")
(aset l "transports" "file" "format" "{h}:{i}:{s}:{ms} {text}")
(aset l "transports" "file" "file" "/tmp/iWasWhere-electron.log")

(nodejs/enable-util-print!)

(defn info
  [& args]
  (apply l/info args))

(defn error
  [& args]
  (apply l/error args))

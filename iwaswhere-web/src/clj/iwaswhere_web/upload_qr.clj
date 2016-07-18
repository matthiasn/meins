(ns iwaswhere-web.upload-qr
  "This namespace takes care of rendering the static HTML into which the React / Reagent
  components are mounted on the client side at runtime."
  (:require [compojure.core :refer [GET]]
            [clj.qrgen :as qr])
  (:import (java.net NetworkInterface Inet4Address)))

; ip-filter, ip-extract, and ips functions borrowed from:
; http://software-ninja-ninja.blogspot.de/2013/05/clojure-what-is-my-ip-address.html
(defn ip-filter [inet]
  (and (.isUp inet)
       (not (.isVirtual inet))
       (not (.isLoopback inet))))

(defn ip-extract [netinf]
  (let [inets (enumeration-seq (.getInetAddresses netinf))]
    (map #(vector (.getHostAddress %1) %2)
         (filter #(instance? Inet4Address %) inets)
         (repeat (.getName netinf)))))

(defn ips []
  (let [ifc (NetworkInterface/getNetworkInterfaces)]
    (mapcat ip-extract (filter ip-filter (enumeration-seq ifc)))))

(def address-qr-route
  (GET "/upload-address.png" []
    (qr/as-input-stream (qr/from (str "http://" (first (first (ips))) ":3001") :size [300 300]))))

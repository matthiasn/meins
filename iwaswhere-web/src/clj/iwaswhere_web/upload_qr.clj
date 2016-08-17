(ns iwaswhere-web.upload-qr
  "Functions for rendering a QR code that contains the IP address for upload."
  (:require [compojure.core :refer [GET]]
            [clj.qrgen :as qr]
            [iwaswhere-web.upload :as up])
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
    (qr/as-input-stream
      (let [ip (ffirst (ips))]
        (qr/from (str "http://" ip ":" up/upload-port "/upload/")
                 :size [300 300])))))

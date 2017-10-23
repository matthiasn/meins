(ns iwaswhere-web.routes.upload-qr
  "Functions for rendering a QR code that contains the IP address for upload."
  (:require [compojure.core :refer [GET]]
            [clj.qrgen :as qr]
            [iwaswhere-web.upload :as up]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [iwaswhere-web.net :as net]))

(def address-qr-route
  (GET "/upload-address/:uuid/qrcode.png" [_uuid]
    (qr/as-input-stream
      (let [ip (ffirst (net/ips))
            url (str "http://" ip ":" @up/upload-port "/upload/")]
        (log/info "QR Code for:" url)
        (qr/from url :size [300 300])))))

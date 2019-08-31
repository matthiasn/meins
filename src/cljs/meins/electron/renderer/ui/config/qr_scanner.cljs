(ns meins.electron.renderer.ui.config.qr-scanner
  (:require [moment]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs.tools.reader.edn :as edn]
            ["@zxing/library" :refer [BrowserQRCodeReader]]
            [meins.electron.renderer.ui.re-frame.db :refer [emit]]
            [taoensso.timbre :refer-macros [info error]]
            [matthiasn.systems-toolbox.component :as stc]
            [clojure.pprint :as pp]))

(defn stop-scanning [local]
  (some-> (:qr-reader @local)
          (.-stream)
          (.getTracks)
          (aget 0)
          (.stop)))

(defn did-mount [local _]
  (let [qr-reader (BrowserQRCodeReader.)
        on-scanned (fn [qr]
                     (try
                       (let [data (edn/read-string (.-text qr))]
                         (info "Scanned data:" data)
                         (swap! local assoc :scanned data)
                         (stop-scanning local))
                       (catch :default e (error e))))
        scan (fn [cameras]
               (when (pos? (.-length cameras))
                 (let [cam-id (aget cameras 0 "deviceId")]
                   (-> qr-reader
                       (.decodeFromInputVideoDevice cam-id "qr-video")
                       (.then on-scanned)
                       (.catch #(js/console.error %))))))]
    (swap! local assoc :qr-reader qr-reader)
    (js/console.info qr-reader)
    (-> (.listVideoInputDevices qr-reader)
        (.then scan))
    (info "QR scanner did mount")))

(defn qr [local _]
  [:div
   (if-let [s (:scanned @local)]
     [:div.scanned
      [:pre [:code (with-out-str (pp/pprint s))]]]
     [:video#qr-video])])

(defn will-unmount [local _]
  (info "QR scanner will unmount")
  (stop-scanning local))

(defn scanner []
  (let [local (r/atom {:local "foo"})]
    (r/create-class
      {:component-did-mount    (partial did-mount local)
       :component-will-unmount (partial will-unmount local)
       :display-name           "QR-Scanner"
       :reagent-render         (partial qr local)})))
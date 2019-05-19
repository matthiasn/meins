(ns meins.ios.sync
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [glittershark.core-async-storage :as as]
            [cljs.core.async :refer [<!]]
            [clojure.string :as s]
            [meins.ui.shared :as shared]
            [cljs.reader :as edn]
            [clojure.pprint :as pp]
            [re-frame.core :refer [subscribe]]
            ["crypto-js" :as crypto-js]
            ["buffer" :as buffer]
            ["@matthiasn/react-native-mailcore" :as react-native-mailcore]
            [meins.ui.db :as uidb]
            [cljs.reader :as rdr]))

(def AES (aget crypto-js "AES"))
(def utf-8 (aget crypto-js "enc" "Utf8"))

(def Buffer (aget buffer "Buffer"))

(defn buffer-convert [from to s]
  (let [buffer (.from Buffer s from)]
    (.toString buffer to)))

(defn utf8-to-hex [s] (buffer-convert "utf-8" "hex" s))
(defn hex-to-utf8 [s] (buffer-convert "hex" "utf-8" s))

(def MailCore (.-default react-native-mailcore))

(defn extract-body [s]
  (-> (str s)
      (s/split "-")
      first
      (s/replace " " "")
      (s/replace "=\r\n" "")
      (s/replace "\r\n" "")
      (s/replace "\n" "")))

(defn decrypt-body [body secret]
  (try
    (let [cleaned (extract-body body)
          ciphertext (hex-to-utf8 cleaned)
          decrypted-bytes (.decrypt AES ciphertext secret)
          s (.toString decrypted-bytes utf-8)
          data (edn/read-string s)]
      data)
    (catch :default e (shared/alert (str "decrypt body " e)))))

(defn sync-write [{:keys [msg-type msg-payload put-fn cmp-state db-item]}]
  (when-let [secrets (:secrets @cmp-state)]
    (try
      (let [aes-secret (-> secrets :sync :write :secret)
            folder (-> secrets :sync :write :folder)
            ts (:timestamp msg-payload)

            ; actual meta-data too large, makes the encryption waste battery
            msg-meta {}
            serializable [msg-type {:msg-payload msg-payload
                                    :msg-meta    msg-meta}]
            data (pr-str serializable)
            ciphertext (.toString (.encrypt AES data aes-secret))
            hex-cipher (utf8-to-hex ciphertext)

            photo-uri (-> msg-payload :media :image :uri)
            filename (:img_file msg-payload)
            audiofile (:audio_file msg-payload)
            mb (-> secrets :server :username)
            mail (merge (:server secrets)
                        {:folder   folder
                         :from     {:addressWithDisplayName mb
                                    :mailbox                mb}
                         :to       {:addressWithDisplayName mb
                                    :mailbox                mb}
                         :subject  (str msg-type)
                         :textBody hex-cipher}
                        (when audiofile {:audiofile audiofile})
                        (when (and (= :entry/sync msg-type) filename)
                          {:attachmentUri photo-uri
                           :filename      filename}))
            success-cb (fn []
                         (when db-item
                           (.write @uidb/realm-db #(set! (.-sync db-item) "DONE"))
                           (put-fn [:schedule/new {:timeout 100
                                                   :message [:sync/retry]
                                                   :id      :sync}])))]
        (swap! cmp-state update-in [:open-writes] conj msg-payload)
        (-> (.saveImap MailCore (clj->js mail))
            (.then success-cb)
            (.catch #(js/console.error (str (js->clj %))))))
      (catch :default e (js/console.error (str e)))))
  {})

(defn schedule-read [cmp-state put-fn]
  (when (seq (:not-fetched @cmp-state))
    (put-fn [:schedule/new
             {:timeout 1000
              :message [:sync/read]}])))

(defn sync-get-uids [{:keys [put-fn cmp-state current-state]}]
  (when-let [secrets (:secrets @cmp-state)]
    (try
      (let [folder (-> secrets :sync :read :folder)
            min-uid (or (last (:not-fetched current-state))
                        (:last-uid-read current-state))
            mail (merge (:server secrets)
                        {:folder folder
                         :minUid min-uid
                         :length 100})
            fetch-cb (fn [data]
                       (let [uids (edn/read-string (str "[" data "]"))]
                         (swap! cmp-state update :not-fetched into uids)
                         ;(shared/alert (:not-fetched @cmp-state))
                         (schedule-read cmp-state put-fn)))]
        (-> (.fetchImap MailCore (clj->js mail))
            (.then fetch-cb)
            (.catch #(shared/alert (str %)))))
      (catch :default e (shared/alert (str e)))))
  {})

(defn sync-read-msg [{:keys [put-fn cmp-state]}]
  (when-let [secrets (:secrets @cmp-state)]
    (try
      (let [{:keys [fetched not-fetched]} @cmp-state
            not-fetched (drop-while #(contains? fetched %) not-fetched)]
        (when-let [uid (first not-fetched)]
          (let [aes-secret (-> secrets :sync :read :secret)
                folder (-> secrets :sync :read :folder)
                mail (merge (:server secrets)
                            {:folder folder
                             :uid    uid})
                fetch-cb (fn [data]
                           (let [body (get (js->clj data) "body")
                                 decrypted (decrypt-body body aes-secret)
                                 msg-type (first decrypted)
                                 {:keys [msg-payload msg-meta]} (second decrypted)
                                 msg (with-meta [msg-type msg-payload] msg-meta)]
                             (swap! cmp-state assoc-in [:last-uid-read] uid)
                             (go (<! (as/set-item :last-uid-read uid)))
                             (swap! cmp-state update-in [:not-fetched] disj uid)
                             (swap! cmp-state update-in [:fetched] conj uid)
                             (schedule-read cmp-state put-fn)
                             ;(shared/alert (with-out-str (pp/pprint msg)))
                             (put-fn msg)))]
            (-> (.fetchImapByUid MailCore (clj->js mail))
                (.then fetch-cb)
                (.catch #(.log js/console (str (js->clj %))))))))
      (catch :default e (shared/alert (str e)))))
  {})

(defn retry-write [{:keys [cmp-state put-fn]}]
  (let [cfg (subscribe [:cfg])
        res (some-> @uidb/realm-db
                    (.objects "Entry")
                    (.filtered "sync == \"OPEN\"")
                    (.slice 0 1))]
    (when (:sync-active @cfg)
      (doseq [x res]
        (sync-write {:db-item     x
                     :cmp-state   cmp-state
                     :put-fn      put-fn
                     :msg-type    :entry/sync
                     :msg-payload (rdr/read-string (aget x "edn"))}))))
  {})

(defn set-secrets [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:secrets] msg-payload)]
    (go (<! (as/set-item :secrets msg-payload)))
    {:new-state new-state}))

(defn state-fn [put-fn]
  (let [state (atom {:last-uid-read 1
                     :not-fetched   (sorted-set)
                     :open-writes   #{}
                     :fetched       #{}})]
    (go (try
          (let [secrets (second (<! (as/get-item :secrets)))]
            (when secrets
              (swap! state assoc-in [:secrets] secrets)))
          (catch js/Object e
            (put-fn [:debug/error {:msg e}]))))
    (go (try
          (let [uid (second (<! (as/get-item :last-uid-read)))]
            (when uid
              (swap! state assoc-in [:last-uid-read] uid)))
          (catch js/Object e
            (put-fn [:debug/error {:msg e}]))))
    {:state state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:entry/sync  sync-write
                 ;:sync/fetch  sync-get-uids
                 :sync/retry  retry-write
                 ;:sync/read   sync-read-msg
                 :secrets/set set-secrets}})
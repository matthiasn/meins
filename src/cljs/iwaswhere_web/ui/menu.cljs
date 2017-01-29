(ns iwaswhere-web.ui.menu
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [re-frame.core :refer [reg-event-db path reg-sub dispatch
                                   dispatch-sync subscribe]]
            [matthiasn.systems-toolbox.component :as stc]))

(defn toggle-option-view
  "Render button for toggle option."
  [{:keys [option cls]} put-fn]
  (let [cfg (subscribe [:cfg])]
    (fn toggle-option-render [{:keys [option cls]} put-fn]
      (let [show-option? (option @cfg)
            toggle-option #(put-fn [:cmd/toggle-key {:path [:cfg option]}])]
        [:span.fa.toggle
         {:class    (str cls (when-not show-option? " inactive"))
          :on-click toggle-option}]))))

(def toggle-options
  [{:option :show-pvt :cls "fa-user-secret"}
   {:option :redacted :cls "fa-eye"}
   {:option :comments-standalone :cls "fa-comments"}
   {:option :mute :cls "fa-volume-off"}
   {:option :hide-hashtags :cls "fa-hashtag"}
   {:option :show-all-maps :cls "fa-map-o"}
   {:option :thumbnails :cls "fa-photo"}
   {:option :reconfigure-grid :cls "fa-arrows"}
   {:option :sort-asc :cls " fa-sort-asc"}])

(defn new-import-view
  "Renders new and import buttons."
  [put-fn]
  [:div
   [:button.menu-new {:on-click (h/new-entry-fn put-fn {} nil)}
    [:span.fa.fa-plus-square] " new"]
   [:button.menu-new
    {:on-click (h/new-entry-fn put-fn {:entry-type :story} nil)}
    [:span.fa.fa-plus-square] " new story"]
   [:button {:on-click #(do (put-fn [:import/photos])
                            (put-fn [:import/geo])
                            (put-fn [:import/weight])
                            (put-fn [:import/phone]))}
    [:span.fa.fa-map] " import"]])

(defn cfg-view
  "Renders component for toggling display of options such as maps, comments.
   The options, with their respective config key and Font-Awesome icon classes
   are defined in the toggle-options vector above. The value for each is then
   set on the application's config, which is persisted in localstorage.
   The default is always false, as initially the key would not be defined at
   all (unless set in default-config)."
  [put-fn]
  (let [cfg (subscribe [:cfg])]
    (fn [put-fn]
      (let [refresh-cfg #(put-fn [:cfg/refresh])
            toggle-qr-code
            (fn [_ev]
              (let [msg {:path [:cfg :qr-code]}
                    reset-msg (merge msg {:reset-to false})]
                (prn :toggle-qr-code)
                (put-fn [:cmd/schedule-new {:timeout 20000
                                            :message [:cmd/toggle-key reset-msg]}])
                (put-fn [:cmd/toggle-key (merge msg {:reset-to true})])))]
        [:div
         (for [option toggle-options]
           ^{:key (str "toggle" (:cls option))}
           [toggle-option-view option put-fn])
         [:span.fa.fa-refresh.toggle {:on-click refresh-cfg}]
         [:span.fa.fa-qrcode.toggle
          {:on-click toggle-qr-code
           :class    (when-not (:qr-code @cfg) "inactive")}]]))))

(defn upload-view
  "Renders QR-code with upload address."
  []
  (let [cfg (subscribe [:cfg])]
    (fn upload-view2-render []
      (when (:qr-code @cfg)
        [:img {:src (str "/upload-address/" (stc/make-uuid) "/qrcode.png")}]))))

(defn menu-view
  "Renders component for rendering new and import buttons."
  [put-fn]
  [:div.menu-header
   [new-import-view put-fn]
   [:h1 "iWasWhere?"]
   [cfg-view put-fn]
   [upload-view]])

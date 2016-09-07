(ns iwaswhere-web.ui.menu
  (:require [iwaswhere-web.helpers :as h]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox.component :as stc]))

(defn toggle-option-view
  "Render button for toggle option."
  [{:keys [option cls]} cfg put-fn]
  (let [show-option? (option cfg)
        toggle-option #(put-fn [:cmd/toggle-key {:path [:cfg option]}])]
    [:span.fa.toggle
     {:class    (str cls (when-not show-option? " inactive"))
      :on-click toggle-option}]))

(def toggle-options
  [{:option :show-pvt :cls "fa-user-secret"}
   {:option :redacted :cls "fa-eye"}
   {:option :comments-standalone :cls "fa-comments"}
   {:option :mute :cls "fa-volume-off"}
   {:option :hide-hashtags :cls "fa-hashtag"}
   {:option :show-all-maps :cls "fa-map-o"}
   {:option :thumbnails :cls "fa-photo"}
   {:option :split-view :cls "fa-columns"}
   {:option :sort-asc :cls " fa-sort-asc"}])

(defn cfg-view
  "Renders component for toggling display of options such as maps, comments.
   The options, with their respective config key and Font-Awesome icon classes
   are defined in the toggle-options vector above. The value for each is then
   set on the application's config, which is persisted in localstorage.
   The default is always false, as initially the key would not be defined at
   all (unless set in default-config)."
  [snapshot put-fn]
  (let [cfg (:cfg snapshot)
        sort-by-upvotes? (:sort-by-upvotes cfg)
        toggle-upvotes
        (fn [_ev]
          (let [query (merge (:current-query snapshot)
                             {:sort-by-upvotes (not sort-by-upvotes?)})]
            (put-fn [:cmd/toggle-key {:path [:cfg :sort-by-upvotes]}])
            (put-fn [:state/search query])))
        toggle-qr-code
        (fn [_ev]
          (let [msg {:path [:cfg :qr-code]}
                reset-msg (merge msg {:reset-to false})]
            (put-fn [:cmd/schedule-new {:timeout 20000
                                        :message [:cmd/toggle-key reset-msg]}])
            (put-fn [:cmd/toggle-key msg])))]
    [:div
     [:span.fa.fa-thumbs-up.toggle
      {:class    (when-not sort-by-upvotes? "inactive")
       :on-click toggle-upvotes}]
     (for [option toggle-options]
       ^{:key (str "toggle" (:cls option))}
       [toggle-option-view option cfg put-fn])
     [:span.fa.fa-qrcode.toggle
      {:on-click toggle-qr-code :class (when-not (:qr-code cfg) "inactive")}]
     [:span.fa.fa-ellipsis-h.toggle
      {:on-click #(put-fn [:cmd/toggle-lines])}]]))

(defn new-import-view
  "Renders new and import buttons."
  [put-fn]
  [:div
   [:button.menu-new {:on-click (h/new-entry-fn put-fn {})}
    [:span.fa.fa-plus-square] " new"]
   [:button {:on-click #(do (put-fn [:import/photos])
                            (put-fn [:import/geo])
                            (put-fn [:import/weight])
                            (put-fn [:import/phone]))}
    [:span.fa.fa-map] " import"]])

(defn upload-view
  "Renders QR-code with upload address."
  [cfg]
  (when (:qr-code cfg)
    [:img {:src (str "/upload-address/" (stc/make-uuid) "/qrcode.png")}]))

(defn menu-view
  "Renders component for rendering new and import buttons."
  [{:keys [observed put-fn]}]
  (let [snapshot @observed
        cfg (:cfg snapshot)]
    [:div.menu-header
     [new-import-view put-fn]
     [:h1 "iWasWhere?"]
     [cfg-view snapshot put-fn]
     [upload-view cfg]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn menu-view
              :dom-id  "header"}))

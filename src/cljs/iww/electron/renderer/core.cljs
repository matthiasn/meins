(ns iww.electron.renderer.core
  (:require [iwaswhere-web.specs]
            [iww.electron.renderer.log]
            [iwaswhere-web.client-store :as store]
            [iww.electron.renderer.ui.re-frame :as rf]
            [iww.electron.renderer.router :as router]
            [iww.electron.renderer.screenshot :as screenshot]
            [iww.electron.renderer.spellcheck :as spellcheck]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [iww.electron.renderer.exec :as exec]
            [cljs.nodejs :as nodejs :refer [process]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defonce switchboard (sb/component :renderer/switchboard))

(def sente-cfg {:relay-types #{:entry/update :entry/find :entry/trash
                               :import/geo :import/photos :import/phone
                               :import/spotify :import/flight :export/pdf
                               :stats/pomo-day-get :import/screenshot
                               :stats/get :stats/get2 :import/movie
                               :state/stats-tags-get :import/weight :import/listen
                               :state/search :cfg/refresh :firehose/cmp-recv
                               :firehose/cmp-put}
                :sente-opts  {:host     (.-iwwHOST js/window)
                              :protocol "http:"}})

(def OBSERVER (.-OBSERVER js/window))

(defn make-observable [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (set (mapv mapper components)))
    components))

(defn start []
  (info "Starting SYSTEM")
  (let [components #{(ipc/cmp-map :renderer/ipc-cmp #{:app/open-external
                                                      :geonames/lookup
                                                      :window/hide
                                                      :blink/busy})
                     (spellcheck/cmp-map :renderer/spellcheck)
                     (screenshot/cmp-map :renderer/screenshot)
                     (sente/cmp-map :renderer/ws-cmp sente-cfg)
                     (router/cmp-map :renderer/router)
                     (store/cmp-map :renderer/store)
                     (sched/cmp-map :renderer/scheduler)
                     (rf/cmp-map :renderer/ui-cmp)
                     (exec/cmp-map :renderer/exec-cmp #{})}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :renderer/ipc-cmp
                    :to   #{:renderer/exec-cmp
                            :renderer/store
                            :renderer/screenshot
                            :renderer/spellcheck
                            :renderer/ws-cmp}}]

       [:cmd/route {:from #{:renderer/router
                            :renderer/scheduler}
                    :to   :renderer/store}]

       [:cmd/route {:from :renderer/store
                    :to   #{:renderer/router
                            :renderer/scheduler}}]

       [:cmd/route {:from #{:renderer/ui-cmp
                            :renderer/store}
                    :to   #{:renderer/ws-cmp
                            :renderer/ipc-cmp}}]

       [:cmd/route {:from #{:renderer/ui-cmp
                            :renderer/ws-cmp}
                    :to   #{:renderer/store
                            :renderer/screenshot
                            :renderer/ipc-cmp}}]

       [:cmd/route {:from :renderer/screenshot
                    :to   #{:renderer/ipc-cmp
                            :renderer/store}}]

       [:cmd/observe-state {:from :renderer/store
                            :to   :renderer/ui-cmp}]

       [:cmd/observe-state {:from :renderer/store
                            :to   :renderer/screenshot}]

       (when OBSERVER [:cmd/attach-to-firehose :renderer/ws-cmp])])))

(defn load-handler [ev]
  (info "RENDERER loaded")
  (start))

(.addEventListener js/window "load" load-handler)

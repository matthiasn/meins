(ns iwaswhere-electron.menu
  (:require [iwaswhere-electron.log :as log]
            [electron :refer [app Menu]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [cljs.nodejs :as nodejs :refer [process]]))

(defn state-fn
  [put-fn]
  (let [menu-tpl
        [{:label   "Application"
          :submenu [{:label    "About iWasWhere"
                     :selector "orderFrontStandardAboutPanel:"}
                    {:label "test"
                     :click #(put-fn [:cmd/test "click"])}
                    {:label       "Quit",
                     :accelerator "Cmd+Q",
                     :click       (fn [_]
                                    (log/info "Shutting down")
                                    (.quit app))}]}
         {:label   "View"
          :submenu [{:label       "New Window"
                     :accelerator "Cmd+N",
                     :click       #(put-fn [:window/new "main"])}
                    {:label "Open Dev Tools"
                     :click #(put-fn [:window/dev-tools])}
                    {:label "Hide Menu"
                     :click #(put-fn [:window/send
                                      {:cmd-type "cmd" :cmd "hide"}])}]}]
        menu (.buildFromTemplate Menu (clj->js menu-tpl))]
    (log/info "Starting Menu Component")
    (.setApplicationMenu Menu menu)))

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

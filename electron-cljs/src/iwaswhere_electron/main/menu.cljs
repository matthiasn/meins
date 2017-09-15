(ns iwaswhere-electron.main.menu
  (:require [taoensso.timbre :as timbre :refer-macros [info]]
            [electron :refer [app Menu]]
            [cljs.nodejs :as nodejs :refer [process]]))

(defn app-menu [put-fn]
  (let [update-win {:url "updater.html" :width 600 :height 300}
        check-updates #(put-fn [:window/new update-win])]
    {:label   "Application"
     :submenu [{:label    "About iWasWhere"
                :selector "orderFrontStandardAboutPanel:"}
               {:label "Check for Updates..."
                :click check-updates}
               {:label   "Clear Caches"
                :submenu [{:label "Clear Electron Cache"
                           :click #(put-fn [:app/clear-cache])}
                          {:label "Clear iWasWhere Snapshot"
                           :click #(put-fn [:app/clear-iww-cache])}]}
               {:label       "Close Window"
                :accelerator "CmdOrCtrl+W"
                :click       #(put-fn [:window/close])}
               {:label "Start Spotify Service"
                :click #(put-fn [:spotify/start])}
               {:label "Start Geocoder Service"
                :click #(put-fn [:geocoder/start])}
               {:label "Quit Background Service"
                :click #(do (put-fn [:app/shutdown-jvm])
                            (put-fn [:app/shutdown]))}
               {:label       "Quit"
                :accelerator "CmdOrCtrl+Q"
                :click       #(put-fn [:app/shutdown])}]}))

(defn file-menu [put-fn]
  (let [new-entry #(put-fn [:exec/js "iwaswhere_web.ui.menu.new_entry()"])
        new-story #(put-fn [:exec/js "iwaswhere_web.ui.menu.new_story()"])
        new-saga #(put-fn [:exec/js "iwaswhere_web.ui.menu.new_saga()"])
        screenshot #(put-fn [:exec/js "iwaswhere_web.ui.menu.capture_screen()"])]
    {:label   "File"
     :submenu [{:label       "New Entry"
                :accelerator "CmdOrCtrl+N"
                :click       new-entry}
               {:label "New Story" :click new-story}
               {:label "New Saga" :click new-saga}
               {:label       "New Screenshot"
                :accelerator "CmdOrCtrl+P"
                :click       screenshot}
               {:label       "Upload"
                :accelerator "CmdOrCtrl+U"
                :click       #(put-fn [:import/listen])}]}))

(defn edit-menu [put-fn]
  (let [lang (fn [cc label]
               (let [cmd (str "window.spellCheckHandler.switchLanguage('" cc "');")]
                 {:label label
                  :click #(put-fn [:window/send {:cmd cmd :cmd-type "cmd"}])}))
        no-spellcheck #(put-fn
                         [:window/send
                          {:cmd      "window.spellCheckHandler.currentSpellchecker=null;;"
                           :cmd-type "cmd"}])]
    {:label   "Edit"
     :submenu [{:label       "Undo"
                :accelerator "CmdOrCtrl+Z"
                :selector    "undo:"}
               {:label       "Redo"
                :accelerator "Shift+CmdOrCtrl+Z"
                :selector    "redo:"}
               {:label       "Cut"
                :accelerator "CmdOrCtrl+X"
                :selector    "cut:"}
               {:label       "Copy"
                :accelerator "CmdOrCtrl+C"
                :selector    "copy:"}
               {:label       "Paste"
                :accelerator "CmdOrCtrl+V"
                :selector    "paste:"}
               {:label       "Select All"
                :accelerator "CmdOrCtrl+A"
                :selector    "selectAll:"}
               {:label   "Spelling"
                :submenu [(lang "en-US" "English")
                          (lang "fr-FR" "French")
                          (lang "de-DE" "German")
                          (lang "it-IT" "Italian")
                          (lang "es-ES" "Spanish")
                          {:type "separator"}
                          {:label "none" :click no-spellcheck}]}]}))

(defn view-menu [put-fn]
  (let [open-window (fn [location]
                      (fn []
                        (put-fn [:window/new {:url "index.html"}])
                        (put-fn
                          [:window/send
                           {:cmd      (str "window.location = '" location "'")
                            :cmd-type "cmd"}])))]
    {:label   "View"
     :submenu [{:label       "New Window"
                :accelerator "CmdOrCtrl+Alt+N"
                :click       (open-window "/#/")}
               {:label "Charts"
                :click (open-window "/#/charts1")}
               {:label "Countries"
                :click (open-window "/#/countries")}
               {:label "Dashboards"
                :click (open-window "/#/dashboards/dashboard-1")}
               {:label       "Toggle Split View"
                :accelerator "CmdOrCtrl+Alt+S"
                :click       #(put-fn [:cmd/toggle-key {:path [:cfg :single-column]}])}
               {:label       "Open Dev Tools"
                :accelerator "CmdOrCtrl+Alt+I"
                :click       #(put-fn [:window/dev-tools])}]}))

(defn state-fn [put-fn]
  (let [put-fn (fn [msg]
                 (let [msg-meta (merge {:window-id :active} (meta msg))]
                   (put-fn (with-meta msg msg-meta))))
        menu-tpl [(app-menu put-fn)
                  (file-menu put-fn)
                  (edit-menu put-fn)
                  (view-menu put-fn)]
        menu (.buildFromTemplate Menu (clj->js menu-tpl))
        activate #(put-fn [:window/activate])]
    (info "Starting Menu Component")
    (.on app "activate" activate)
    (.setApplicationMenu Menu menu))
  {:state (atom {})})

(defn cmp-map [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})

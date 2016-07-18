(ns iwaswhere-web.admin-gui
  (:require [seesaw.core :as sc]
            [seesaw.mig :as sm]))

(defn show-ui
  []
  (sc/show!
    (sc/frame :title "iWasWhere" :width 340 :height 170
              :content
              (sm/mig-panel :items
                            [["General" "split, span, gaptop 5"]
                             [:separator "growx, wrap, gaptop 10"]
                             ["Data dir:" "gap 10"]
                             [(sc/text :columns 20) "span, growx"]
                             ["Network" "split, span, gaptop 10"]
                             [:separator "growx, wrap, gaptop 10"]
                             ["Port:" "gap 10"]
                             [(sc/text :columns 5) ""]
                             ["" "gap 10"]
                             [(sc/button :text "Start")]]))))

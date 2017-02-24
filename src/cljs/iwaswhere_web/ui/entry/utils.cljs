(ns iwaswhere-web.ui.entry.utils
  (:require [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]))

(defn entry-reaction
  [ts]
  (let [new-entries (subscribe [:new-entries])
        entries-map (subscribe [:entries-map])
        combined-entries (subscribe [:combined-entries])
        entry (reaction (get-in @combined-entries [ts]))
        edit-mode (reaction (contains? @new-entries ts))]
    {:entry       entry
     :entries-map entries-map
     :combined-entries combined-entries
     :new-entries new-entries
     :edit-mode   edit-mode}))

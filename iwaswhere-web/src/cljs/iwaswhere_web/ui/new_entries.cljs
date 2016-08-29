(ns iwaswhere-web.ui.new-entries
  (:require [iwaswhere-web.ui.entry :as e]))

(defn new-entries-view
  "Renders view for editing new entries. New entries are those that are only
   persisted in localstorage and not yet in the backend."
  [snapshot local-cfg put-fn]
  (let [cfg (merge (:cfg snapshot) (:options snapshot))
        entries-map (:entries-map snapshot)
        new-entries (:new-entries snapshot)]
    (when (seq new-entries)
      [:div.new-entries
       (for [entry (filter #(and
                             (not (:comment-for %))
                             (not (contains? entries-map (:timestamp %))))
                           (vals new-entries))]
         ^{:key (:timestamp entry)}
         [e/entry-with-comments entry
          cfg new-entries put-fn entries-map local-cfg])])))

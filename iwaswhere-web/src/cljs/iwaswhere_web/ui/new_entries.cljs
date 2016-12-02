(ns iwaswhere-web.ui.new-entries
  (:require [iwaswhere-web.ui.entry.entry :as e]
            [re-frame.core :refer [reg-event-db path reg-sub dispatch
                                   dispatch-sync subscribe]]))

(defn new-entries-view
  "Renders view for editing new entries. New entries are those that are only
   persisted in localstorage and not yet in the backend."
  [put-fn]
  (let [entries-map (subscribe [:entries-map])
        new-entries (subscribe [:new-entries])]
    (fn new-entries-render [put-fn]
      (let [entries-map @entries-map
            new-entries @new-entries
            filtered-entries (filter #(and
                                        (not (:comment-for %))
                                        (not (contains? entries-map (:timestamp %))))
                                     (vals new-entries))]
        (when (seq filtered-entries)
          [:div.new-entries
           [:div.new-entries-list
            (for [entry filtered-entries]
              ^{:key (:timestamp entry)}
              [e/entry-with-comments entry put-fn {}])]])))))

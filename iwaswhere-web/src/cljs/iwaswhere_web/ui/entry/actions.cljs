(ns iwaswhere-web.ui.entry.actions
  (:require [iwaswhere-web.ui.pomodoro :as p]
            [iwaswhere-web.utils.parse :as up]
            [cljsjs.moment]
            [iwaswhere-web.helpers :as h]
            [reagent.core :as r]))

(defn trash-icon
  "Renders a trash icon, which transforms into a warning button that needs to be
   clicked again for actual deletion. This label is a little to the right, so it
   can't be clicked accidentally, and disappears again within 5 seconds."
  [trash-fn]
  (let [clicked (r/atom false)
        guarded-trash-fn (fn [_ev]
                           (swap! clicked not)
                           (.setTimeout js/window #(reset! clicked false) 5000))]
    (fn [trash-fn]
      (if @clicked
        [:span.delete-warn {:on-click trash-fn}
         [:span.fa.fa-trash] "  confirm delete?"]
        [:span.fa.fa-trash-o.toggle {:on-click guarded-trash-fn}]))))

(defn edit-icon
  "Renders an edit icon, which transforms into a warning button that needs to be
   clicked again for actually discarding changes. This label is a little to the
   right, so it can't be clicked accidentally, and disappears again within 5
   seconds."
  [toggle-edit edit-mode? entry]
  (let [clicked (r/atom false)
        guarded-edit-fn (fn [_ev]
                          (swap! clicked not)
                          (.setTimeout js/window #(reset! clicked false) 5000))]
    (fn [toggle-edit edit-mode? entry]
      (if edit-mode?
        (if @clicked
          (let [discard-click-fn #(do (toggle-edit)
                                      (swap! clicked not)
                                      (prn "Discarding local changes:" entry))]
            [:span.delete-warn {:on-click discard-click-fn}
             [:span.fa.fa-trash] "  discard changes?"])
          [:span.fa.fa-pencil-square-o.toggle {:on-click guarded-edit-fn}])
        [:span.fa.fa-pencil-square-o.toggle {:on-click toggle-edit}]))))

(defn drop-linked-fn
  "Creates handler function for drop event, which takes the timestamp of the
   currently dragged element and links that entry to the one onto which it is
   dropped."
  [entry cfg put-fn]
  (fn [_ev]
    (let [ts (:currently-dragged cfg)
          new-entry (update-in entry [:linked-entries] #(set (conj % ts)))]
      (put-fn [:entry/update (h/clean-entry new-entry)]))))

(defn new-link
  "Renders input for adding link entry."
  [entry put-fn create-linked-entry]
  (let [local (r/atom {:visible false})
        toggle-visible #(swap! local update-in [:visible] not)
        on-drag-start (fn [ev]
                        (let [dt (.-dataTransfer ev)]
                          (put-fn [:cmd/set-dragged entry])
                          (aset dt "effectAllowed" "move")
                          (aset dt "dropEffect" "link")))]
    (fn [entry put-fn create-linked-entry]
      [:span.new-link-btn
       [:span.fa.fa-link.toggle {:on-click      toggle-visible
                                 :draggable     true
                                 :on-drag-start on-drag-start}]
       (when (:visible @local)
         [:span.new-link
          {:on-click #(do (create-linked-entry) (toggle-visible))}
          [:span.fa.fa-plus-square] "add linked"])])))

(defn upvote-fn [entry op put-fn]
  "Create click function for like. Can handle both upvotes and downvotes."
  (fn [_ev]
    (put-fn [:entry/update (update-in entry [:upvotes] op)])))

(defn entry-actions
  "Entry-related action buttons. Hidden by default, become visible when mouse
   hovers over element, stays visible for a little while after mose leaves."
  [entry cfg put-fn edit-mode? toggle-edit local-cfg]
  (let [visible (r/atom false)
        hide-fn (fn [_ev]
                  (.setTimeout js/window #(reset! visible false) 60000))]
    (fn
      [entry cfg put-fn edit-mode? toggle-edit local-cfg]
      (let [ts (:timestamp entry)
            query-id (:query-id local-cfg)
            tab-group (:tab-group local-cfg)
            map? (:latitude entry)
            toggle-map #(put-fn [:cmd/toggle
                                 {:timestamp ts
                                  :path      [:cfg :show-maps-for]}])
            show-hide-comments #(put-fn [:cmd/assoc-in
                                         {:path  [:cfg :show-comments-for ts]
                                          :value %}])
            show-comments #(show-hide-comments query-id)
            create-comment (h/new-entry-fn put-fn {:comment-for ts} show-comments)
            create-linked-entry (h/new-entry-fn put-fn {:linked-entries [ts]} nil)
            new-pomodoro (h/new-entry-fn
                           put-fn (p/pomodoro-defaults ts) show-comments)
            add-activity
            (fn [_ev]
              (put-fn [:entry/update-local
                       (-> entry
                           (assoc-in [:activity]
                                     {:name           ""
                                      :duration-m     0
                                      :exertion-level 5})
                           (update-in [:tags] conj "#activity")
                           (update-in [:md] #(str % " #activity ")))]))
            add-consumption
            (fn [_ev]
              (put-fn [:entry/update-local
                       (-> entry
                           (assoc-in [:consumption]
                                     {:name     ""
                                      :quantity 0})
                           (update-in [:tags] conj "#consumption")
                           (update-in [:md] #(str % " #consumption ")))]))
            trash-entry #(if edit-mode?
                          (put-fn [:entry/remove-local {:timestamp ts}])
                          (put-fn [:entry/trash entry]))
            open-external (up/add-search ts tab-group put-fn)
            upvotes (:upvotes entry)
            show-pvt? (:show-pvt cfg)
            prev-saved? (:last-saved entry)]
        [:div {:on-mouse-enter #(reset! visible true)
               :on-drag-over   #(do (hide-fn nil) (reset! visible true))
               :on-mouse-leave hide-fn
               :style          {:opacity (if (or edit-mode? @visible) 1 0)}}
         (when prev-saved?
           [:span.fa.toggle
            {:on-click (upvote-fn entry inc put-fn)
             :class    (if (pos? upvotes) "fa-thumbs-up" "fa-thumbs-o-up")}])
         (when map? [:span.fa.fa-map-o.toggle {:on-click toggle-map}])
         (when prev-saved?
           [edit-icon toggle-edit edit-mode? entry])
         (when-not (:comment-for entry)
           [:span.fa.fa-clock-o.toggle {:on-click new-pomodoro}])
         (when-not (:activity entry)
           [:span.fa.fa-bicycle.toggle {:on-click add-activity}])
         (when (and show-pvt? (not (:consumption entry)))
           [:span.fa.fa-coffee.toggle {:on-click add-consumption}])
         (when-not (:comment-for entry)
           [:span.fa.fa-comment-o.toggle {:on-click create-comment}])
         (when (and (not (:comment-for entry)) prev-saved?)
           [:span.fa.fa-external-link.toggle {:on-click open-external}])
         (when-not (:comment-for entry)
           [new-link entry put-fn create-linked-entry])
         [trash-icon trash-entry]]))))

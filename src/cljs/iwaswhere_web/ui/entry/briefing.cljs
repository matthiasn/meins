(ns iwaswhere-web.ui.entry.briefing
  (:require [matthiasn.systems-toolbox.component :as st]
            [iwaswhere-web.ui.charts.durations :as p]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [iwaswhere-web.charts.data :as cd]
            [iwaswhere-web.ui.charts.common :as cc]
            [iwaswhere-web.utils.misc :as u]
            [clojure.pprint :as pp]
            [iwaswhere-web.ui.entry.actions :as a]
            [iwaswhere-web.utils.parse :as up]
            [clojure.string :as s]
            [iwaswhere-web.ui.entry.utils :as eu]
            [reagent.core :as r]))

(defn time-by-stories-list
  "Render list of times spent on individual stories, plus the total."
  [day-stats local put-fn]
  (let [stories (subscribe [:stories])
        sagas (subscribe [:sagas])
        saga-filter (fn [[k v]]
                      (if-let [selected (:selected @local)]
                        (let [story (get @stories k)]
                          (= selected (:linked-saga story)))
                        true))
        story-name-mapper (fn [[k v]]
                            (let [s (or (:story-name (get @stories k)) "none")]
                              [k s v]))]
    (fn [day-stats local put-fn]
      (let [stories @stories
            sagas @sagas
            dur (u/duration-string (:total-time day-stats))
            date (:date-string day-stats)
            time-by-story (:time-by-story day-stats)
            time-by-story2 (->> day-stats
                                :time-by-story
                                (filter saga-filter)
                                (map story-name-mapper)
                                (sort-by second))
            y-scale 0.0045]
        (when date
          [:table
           [:tbody
            [:tr [:th ""] [:th "story"] [:th "actual"]]
            (for [[id story v] time-by-story2]
              (let [color (cc/item-color story)
                    q (merge
                        (up/parse-search date)
                        {:story (when-not (js/isNaN id) id)})
                    click-fn (fn [_]
                               (put-fn [:search/add {:tab-group :right
                                                     :query     q}]))]
                ^{:key story}
                [:tr {:on-click click-fn}
                 [:td [:div.legend {:style {:background-color color}}]]
                 [:td [:strong story]]
                 [:td.time (u/duration-string v)]]))]])))))

(defn habit-sorter
  "Sorts tasks."
  [x y]
  (let [c (compare (get-in x [:habit :priority] :X)
                   (get-in y [:habit :priority] :X))]
    (if (not= c 0) c (compare (get-in y [:habit :points])
                              (get-in x [:habit :points])))))

(defn waiting-habits-list
  "Renders table with open entries, such as started tasks and open habits."
  [tab-group entry put-fn]
  (let [cfg (subscribe [:cfg])
        waiting-habits (subscribe [:waiting-habits])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])
        entries-list
        (reaction
          (let [entries-map @entries-map
                find-missing (u/find-missing-entry entries-map put-fn)
                entries (->> @waiting-habits
                             (map (fn [ts] (find-missing ts)))
                             (sort habit-sorter))
                conf (merge @cfg @options)]
            (if (:show-pvt @cfg)
              entries
              (filter (u/pvt-filter conf entries-map) entries))))]
    (fn waiting-habits-list-render [tab-group entry put-fn]
      (let [entries-list @entries-list
            today (.format (js/moment.) "YYYY-MM-DD")
            briefing-day (-> entry :briefing :day)]
        (when (and (= today briefing-day) (seq entries-list))
          [:div
           [:table.habits
            [:tbody
             [:tr
              [:th [:span.fa.fa-exclamation-triangle]]
              [:th [:span.fa.fa-diamond]]
              [:th "waiting habit"]]
             (for [entry entries-list]
               (let [ts (:timestamp entry)]
                 ^{:key ts}
                 [:tr {:on-click (up/add-search ts tab-group put-fn)}
                  [:td
                   (when-let [prio (-> entry :habit :priority)]
                     [:span.prio {:class prio} prio])]
                  [:td.award-points
                   (when-let [points (-> entry :habit :points)]
                     points)]
                  [:td.habit
                   (some-> entry
                           :md
                           (s/replace "#task" "")
                           (s/replace "#habit" "")
                           (s/replace "##" "")
                           s/split-lines
                           first)]]))]]])))))

(defn task-sorter
  "Sorts tasks."
  [x y]
  (let [c (compare (get-in x [:task :priority] :X)
                   (get-in y [:task :priority] :X))]
    (if (not= c 0) c (compare (get-in x [:task :active-from])
                              (get-in y [:task :active-from])))))

(defn started-tasks-list
  "Renders table with open entries, such as started tasks and open habits."
  [tab-group local put-fn]
  (let [cfg (subscribe [:cfg])
        started-tasks (subscribe [:started-tasks])
        options (subscribe [:options])
        entries-map (subscribe [:entries-map])
        options (subscribe [:options])
        stories (reaction (:stories @options))
        not-on-hold #(not (:on-hold (:task %)))
        on-hold-filter (fn [entry]
                         (let [on-hold (:on-hold (:task entry))]
                           (if (:on-hold @local)
                             on-hold
                             (not on-hold))))
        saga-filter (fn [entry]
                      (if-let [selected (:selected @local)]
                        (let [story (get @stories (:linked-story entry))]
                          (= selected (:linked-saga story)))
                        true))
        filter-btn (fn [fk]
                     [:span.filter {:class    (when (:on-hold @local) "current")
                                    :on-click #(swap! local update-in [:on-hold] not)}
                      (name fk)])
        entries-list (reaction
                       (let [entries-map @entries-map
                             find-missing (u/find-missing-entry entries-map put-fn)
                             entries (->> @started-tasks
                                          (map (fn [ts] (find-missing ts)))
                                          (filter on-hold-filter)
                                          (filter saga-filter)
                                          (sort task-sorter))
                             conf (merge @cfg @options)]
                         (if (:show-pvt @cfg)
                           entries
                           (filter (u/pvt-filter conf entries-map) entries))))]
    (fn started-tasks-list-render [tab-group local put-fn]
      (let [entries-list @entries-list]
        (when (seq entries-list)
          [:div.linked-tasks
           [:table.habits
            [:tbody
             [:tr
              [:th ""]
              [:th
               [:div
                "started tasks: "
                [filter-btn :on-hold]]]]
             (for [entry entries-list]
               (let [ts (:timestamp entry)]
                 ^{:key ts}
                 [:tr {:on-click (up/add-search ts tab-group put-fn)}
                  [:td
                   (when-let [prio (-> entry :task :priority)]
                     [:span.prio {:class prio} prio])]
                  [:td
                   [:strong (some-> entry
                                    :md
                                    (s/replace "#task" "")
                                    (s/replace "#habit" "")
                                    (s/replace "##" "")
                                    s/split-lines
                                    first)]]]))]]])))))

(defn open-linked-tasks-list
  "Show open tasks that are also linked with the briefing entry."
  [ts local local-cfg put-fn]
  (let [entry (:entry (eu/entry-reaction ts))
        cfg (subscribe [:cfg])
        results (subscribe [:results])
        options (subscribe [:options])
        stories (reaction (:stories @options))
        entries-map (subscribe [:entries-map])
        linked-filters {:active  (up/parse-search "#task ~#done ~#closed ~#backlog")
                        :open    (up/parse-search "#task ~#done ~#closed ~#backlog")
                        :done    (up/parse-search "#task #done")
                        :closed  (up/parse-search "#task #closed")
                        :backlog (up/parse-search "#task #backlog")}
        filter-btn (fn [fk]
                     [:span.filter {:class    (when (= fk (:filter @local)) "current")
                                    :on-click #(swap! local assoc-in [:filter] fk)}
                      (name fk)])]
    (fn open-linked-tasks-render [ts local local-cfg put-fn]
      (let [{:keys [tab-group query-id]} local-cfg
            linked-entries-set (into (sorted-set) (:linked-entries-list @entry))
            linked-mapper (u/find-missing-entry @entries-map put-fn)
            linked-entries (mapv linked-mapper linked-entries-set)
            conf (merge @cfg @options)
            linked-entries (if (:show-pvt conf)
                             linked-entries
                             (filter (u/pvt-filter conf @entries-map) linked-entries))
            current-filter (get linked-filters (:filter @local))
            filter-fn (u/linked-filter-fn @entries-map current-filter put-fn)
            saga-filter (fn [entry]
                          (if-let [selected (:selected @local)]
                            (let [story (get @stories (:linked-story entry))]
                              (= selected (:linked-saga story)))
                            true))
            active-filter (fn [t]
                            (let [active-from (-> t :task :active-from)
                                  current-filter (get linked-filters (:filter @local))]
                              (if (and active-from (= (:filter @local) :active))
                                (let [from-now (.fromNow (js/moment active-from))]
                                  (s/includes? from-now "ago"))
                                true)))
            linked-entries (->> linked-entries
                                (filter filter-fn)
                                (filter saga-filter)
                                (filter active-filter)
                                (sort-by #(or (-> % :task :priority) :X)))]
        [:div.linked-tasks
         [:table
          [:tbody
           [:tr [:th ""]
            [:th [:div
                  [:strong "tasks:"]
                  [filter-btn :active]
                  [filter-btn :open]
                  [filter-btn :done]
                  [filter-btn :closed]
                  [filter-btn :backlog]]]]
           (for [linked linked-entries]
             (let [ts (:timestamp linked)
                   on-drag-start (a/drag-start-fn linked put-fn)]
               ^{:key ts}
               [:tr {:on-click (up/add-search ts tab-group put-fn)}
                (let [prio (or (-> linked :task :priority) "-")]
                  [:td
                   [:span.prio {:class         prio
                                :draggable     true
                                :on-drag-start on-drag-start}
                    prio]])
                [:td.left [:strong (some-> linked
                                           :md
                                           (s/replace "#task" "")
                                           (s/replace "##" "")
                                           s/trim
                                           s/split-lines
                                           first)]]]))]]]))))

(defn vertical-bar
  "Draws vertical stacked barchart."
  [entities k time-by-entities y-scale]
  (let [data (cd/time-by-entity-stacked time-by-entities)]
    (when (seq time-by-entities)
      [:svg.vertical-bar
       ;{:viewBox (str "0 0 12 300")}
       [:g (for [[entity {:keys [x v]}] data]
             (let [h (* y-scale v)
                   x (* y-scale x)
                   entity-name (or (k (get entities entity)) "none")]
               ^{:key (str entity)}
               [:rect {:fill   (cc/item-color entity-name)
                       :y      x
                       :x      0
                       :width  12
                       :height h}]))]])))

(defn time-by-sagas-list
  [entry day-stats local edit-mode? put-fn]
  (let [options (subscribe [:options])
        sagas (reaction (:sagas @options))
        time-alloc-input-fn
        (fn [entry saga]
          (fn [ev]
            (let [m (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  s (* m 60)
                  updated (assoc-in entry [:briefing :time-allocation saga] s)]
              (put-fn [:entry/update-local updated]))))
        filter-click #(swap! local update-in [:outstanding-time-filter] not)]
    (fn [entry day-stats local edit-mode? put-fn]
      (let [actual-times (:time-by-saga day-stats)
            filtered? (:outstanding-time-filter @local)
            filter-cls (when-not filtered? "inactive")
            sagas (sort-by #(-> % second :saga-name) @sagas)]
        [:table
         [:tbody
          [:tr
           [:th [:span.fa.fa-filter
                 {:on-click filter-click
                  :class filter-cls}]]
           [:th "saga"]
           [:th "planned"]
           [:th "actual"]
           [:th "remaining"]]
          (for [[k v] sagas]
            (let [allocation (get-in entry [:briefing :time-allocation k] 0)
                  actual (get-in actual-times [k] 0)
                  remaining (- allocation actual)
                  color (cc/item-color (:saga-name v))
                  click
                  (fn [_]
                    (when-not edit-mode?
                      (swap! local update-in [:selected] #(if (= k %) nil k))))]
              (when (or (pos? allocation) (get actual-times k) edit-mode?)
                (when (or (not filtered?) (pos? remaining) edit-mode?)
                  ^{:key (str :time-allocation k)}
                  [:tr {:on-click click
                        :class    (when (= k (:selected @local)) "selected")}
                   [:td [:div.legend {:style {:background-color color}}]]
                   [:td [:strong (:saga-name v)]]
                   [:td.time
                    (if edit-mode?
                      [:input {:on-input (time-alloc-input-fn entry k)
                               :value    (when allocation (/ allocation 60))
                               :type     :number}]
                      [:span (u/duration-string allocation)])]
                   [:td.time (u/duration-string actual)]
                   [:td.time [:strong (u/duration-string remaining)]]]))))]]))))

(defn briefing-view
  [entry put-fn edit-mode? local-cfg]
  (let [chart-data (subscribe [:chart-data])
        day (-> entry :briefing :day)
        today (.format (js/moment.) "YYYY-MM-DD")
        filter-btn (if (= day today) :active :open)
        local (r/atom {:filter  filter-btn
                       :outstanding-time-filter true
                       :on-hold false})
        stats (subscribe [:stats])
        options (subscribe [:options])
        sagas (reaction (:sagas @options))
        entries-map (subscribe [:entries-map])
        results (subscribe [:results])
        cfg (subscribe [:cfg])

        input-fn
        (fn [entry]
          (fn [ev]
            (let [day (-> ev .-nativeEvent .-target .-value)
                  updated (assoc-in entry [:briefing :day] day)]
              (put-fn [:entry/update-local updated]))))

        time-alloc-input-fn
        (fn [entry saga]
          (fn [ev]
            (let [m (js/parseInt (-> ev .-nativeEvent .-target .-value))
                  s (* m 60)
                  updated (assoc-in entry [:briefing :time-allocation saga] s)]
              (put-fn [:entry/update-local updated]))))]
    (fn briefing-render [entry put-fn edit-mode? local-cfg]
      (when (contains? (:tags entry) "#briefing")
        (let [sagas @sagas
              ts (:timestamp entry)
              {:keys [pomodoro-stats task-stats wordcount-stats]} @chart-data
              day (-> entry :briefing :day)
              day-stats (get pomodoro-stats day)
              dur (u/duration-string (:total-time day-stats))
              word-stats (get wordcount-stats day)
              {:keys [tasks-cnt done-cnt closed-cnt]} (get task-stats day)
              started (:started-tasks-cnt @stats)
              allocation (-> entry :briefing :time-allocation)
              actual-times (:time-by-saga day-stats)
              remaining (cd/remaining-times actual-times allocation)
              past-7-days (cd/past-7-days pomodoro-stats :time-by-saga)
              tab-group (:tab-group local-cfg)]
          [:div.briefing
           [:form.briefing-details
            [:fieldset
             [:legend (or day "date not set")]
             (when edit-mode?
               [:div
                [:label " Day: "]
                [:input {:type     :date
                         :on-input (input-fn entry)
                         :value    day}]])
             (when tasks-cnt
               [:div
                "Tasks: " [:strong tasks-cnt] " created, "
                [:strong done-cnt] " done, "
                [:strong closed-cnt] " closed. "
                [:strong (or (:word-count word-stats) 0)] " words written."])
             [:div
              "Total planned: "
              [:strong
               (u/duration-string
                 (apply + (map second (-> entry :briefing :time-allocation))))]
              (when (seq dur)
                [:span
                 " Logged: " [:strong dur] " in " (:total day-stats) " entries."])]
             [time-by-sagas-list entry day-stats local edit-mode? put-fn]
             [started-tasks-list tab-group local put-fn]
             [open-linked-tasks-list ts local local-cfg put-fn]
             [waiting-habits-list tab-group entry put-fn]
             (when day-stats [time-by-stories-list day-stats local put-fn])]]
           [:div.stacked-bars
            [:div [vertical-bar sagas :saga-name allocation 0.0045]]
            [:div [vertical-bar sagas :saga-name actual-times 0.0045]]
            [:div [vertical-bar sagas :saga-name remaining 0.0045]]]])))))

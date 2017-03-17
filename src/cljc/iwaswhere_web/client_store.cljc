(ns iwaswhere-web.client-store
  (:require #?(:cljs [alandipert.storage-atom :refer [local-storage]])
    #?(:cljs [reagent.core :refer [atom]])
    #?(:cljs [iwaswhere-web.ui.stats :as stats])
    [matthiasn.systems-toolbox.component :as st]
    [iwaswhere-web.keepalive :as ka]
    [re-frame.db :as rdb]
    [iwaswhere-web.client-store-entry :as cse]
    [iwaswhere-web.client-store-search :as s]
    [iwaswhere-web.client-store-cfg :as c]))

(defn new-state-fn
  "Update client side state with list of journal entries received from backend."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [store-meta (:client/store-cmp msg-meta)
        {:keys [entries entries-map]} msg-payload
        new-state (-> current-state
                      (assoc-in [:results] entries)
                      (update-in [:entries-map] merge entries-map)
                      (assoc-in [:timing] {:query (:duration-ms msg-payload)
                                           :rtt   (- (:in-ts store-meta)
                                                     (:out-ts store-meta))
                                           :count (count entries-map)}))]
    {:new-state new-state}))

(defn stats-tags-fn
  "Update client side state with stats and tags received from backend."
  [{:keys [current-state msg-payload put-fn]}]
  (let [stories (:stories msg-payload)
        sorted-stories (sort (fn [[_ x] [_ y]]
                               (< (:story-name x) (:story-name y)))
                             stories)
        books (:books msg-payload)
        sorted-books (sort (fn [[_ x] [_ y]]
                               (< (:book-name x) (:book-name y)))
                             books)
        new-state
        (-> current-state
            (assoc-in [:options :hashtags] (:hashtags msg-payload))
            (assoc-in [:options :pvt-hashtags] (:pvt-hashtags msg-payload))
            (assoc-in [:options :pvt-displayed] (:pvt-displayed msg-payload))
            (assoc-in [:options :custom-fields] (:custom-fields (:cfg msg-payload)))
            (assoc-in [:options :custom-field-charts] (:custom-field-charts (:cfg msg-payload)))
            (assoc-in [:options :stories] stories)
            (assoc-in [:options :locations] (:locations msg-payload))
            (assoc-in [:options :sorted-stories] sorted-stories)
            (assoc-in [:options :books] books)
            (assoc-in [:options :sorted-books] sorted-books)
            (assoc-in [:options :mentions] (:mentions msg-payload))
            (assoc-in [:briefings] (:briefings msg-payload))
            (assoc-in [:started-tasks] (:started-tasks msg-payload))
            (assoc-in [:waiting-habits] (:waiting-habits msg-payload))
            (assoc-in [:cfg :briefing-story] (:briefing-story (:cfg msg-payload)))
            (assoc-in [:stats] (:stats msg-payload)))]
    #?(:cljs (stats/update-stats put-fn))
    {:new-state new-state}))

(defn initial-state-fn
  "Creates the initial component state atom. Holds a list of entries from the
   backend, a map with temporary entries that are being edited but not saved
   yet, and sets that contain information for which entries to show the map,
   or the edit mode."
  [put-fn]
  (let [initial-state (atom {:entries         []
                             :last-alive      (st/now)
                             :new-entries     @cse/new-entries-ls
                             :query-cfg       @s/query-cfg
                             :pomodoro-stats  (sorted-map)
                             :activity-stats  (sorted-map)
                             :task-stats      (sorted-map)
                             :wordcount-stats (sorted-map)
                             :options         {:pvt-hashtags #{"#pvt"}}
                             :cfg             @c/app-cfg})]
    (put-fn [:state/stats-tags-get])
    (put-fn [:state/search (:query-cfg @initial-state)])
    {:state initial-state}))

(defn save-stats-fn
  "Stores received stats on component state."
  [{:keys [current-state msg-payload]}]
  (let [k (case (:type msg-payload)
            :stats/pomodoro :pomodoro-stats
            :stats/activity :activity-stats
            :stats/tasks :task-stats
            :stats/wordcount :wordcount-stats
            :stats/daily-summaries :daily-summary-stats
            :stats/custom-fields :custom-field-stats
            :stats/media :media-stats
            nil)
        day-stats (into (sorted-map) (:stats msg-payload))]
    (if k
      {:new-state (assoc-in current-state [k] day-stats)}
      (prn "WARN: No key defined for " msg-payload))))

(defn cmp-map
  "Creates map for the component which holds the client-side application state."
  [cmp-id]
  {:cmp-id            cmp-id
   :state-fn          initial-state-fn
   :snapshot-xform-fn #(dissoc % :last-alive)
   :state-spec        :state/client-store-spec
   :handler-map       (merge cse/entry-handler-map
                             s/search-handler-map
                             {:state/new          new-state-fn
                              :stats/result       save-stats-fn
                              :state/stats-tags   stats-tags-fn
                              :cfg/save           c/save-cfg
                              :cmd/toggle-active  c/toggle-active-fn
                              :cmd/toggle         c/toggle-set-fn
                              :cmd/set-opt        c/set-conj-fn
                              :cmd/set-dragged    c/set-currently-dragged
                              :cmd/toggle-key     c/toggle-key-fn
                              :cmd/assoc-in       c/assoc-in-state
                              :cmd/keep-alive     ka/reset-fn
                              :cmd/keep-alive-res ka/set-alive-fn})})

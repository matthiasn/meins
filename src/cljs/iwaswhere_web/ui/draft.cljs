(ns iwaswhere-web.ui.draft
  (:require [matthiasn.systems-toolbox.component :as st]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [iwaswhere-web.utils.parse :as p]
            [iwaswhere-web.utils.misc :as u]))

(defn editor-state-from-text
  [text]
  (let [content-from-text (.createFromText js/Draft.ContentState text)]
    (.createWithContent js/Draft.EditorState content-from-text)))

(defn editor-state-from-raw
  [editor-state]
  (prn editor-state)
  (let [content-from-raw (.convertFromRaw js/Draft editor-state)]
    (.createWithContent js/Draft.EditorState content-from-raw)))

(defn story-mapper
  [[ts story]]
  {:name (:story-name story)
   :id   ts})

(defn on-editor-change
  [update-cb]
  (fn [new-state]
    (let [current-content (.getCurrentContent new-state)
          plain (.getPlainText current-content)
          raw-content (.convertToRaw js/Draft current-content)
          via-json (.parse js/JSON (.stringify js/JSON raw-content))
          new-state (js->clj via-json :keywordize-keys true)]
      (update-cb plain new-state))))

(defn adapt-react-class
  [cls]
  (r/adapt-react-class (aget js/window "deps" cls "default")))

(defn draft-search-field
  [editor-state update-cb]
  (let [editor (adapt-react-class "SearchFieldEditor")
        options (subscribe [:options])
        sorted-stories (reaction (:sorted-stories @options))
        stories-list (reaction (map story-mapper @sorted-stories))
        cfg (subscribe [:cfg])
        mentions (reaction (map (fn [m] {:name m}) (:mentions @options)))
        hashtags (reaction
                   (let [show-pvt? (:show-pvt @cfg)
                         hashtags (:hashtags @options)
                         pvt-hashtags (:pvt-hashtags @options)
                         hashtags (if show-pvt?
                                    (concat hashtags pvt-hashtags)
                                    hashtags)]
                     (map (fn [h] {:name h}) hashtags)))
        on-change (on-editor-change update-cb)]
    (fn [editor-state send-fn]
      [editor {:editorState editor-state
               :spellCheck  true
               :mentions    @mentions
               :hashtags    @hashtags
               :stories     @stories-list
               :onChange    on-change}])))

(defn draft-text-editor
  [editor-state md update-cb]
  (let [editor (adapt-react-class "EntryTextEditor")
        options (subscribe [:options])
        sorted-stories (reaction (:sorted-stories @options))
        stories-list (reaction (map story-mapper @sorted-stories))
        cfg (subscribe [:cfg])
        mentions (reaction (map (fn [m] {:name m}) (:mentions @options)))
        hashtags (reaction
                   (let [show-pvt? (:show-pvt @cfg)
                         hashtags (:hashtags @options)
                         pvt-hashtags (:pvt-hashtags @options)
                         hashtags (if show-pvt?
                                    (concat hashtags pvt-hashtags)
                                    hashtags)]
                     (map (fn [h] {:name h}) hashtags)))]
    (fn [editor-state md send-fn]
      [editor {:editorState editor-state
               :md          md
               :spellCheck  true
               :mentions    @mentions
               :hashtags    @hashtags
               :stories     @stories-list
               :onChange    update-cb}])))

(defn entry-editor
  [entry put-fn]
  (let [editor-state (when-let [editor-state (:editor-state @entry)]
                       (editor-state-from-raw (clj->js editor-state)))
        local (r/atom {:editor-state (:editor-state @entry)})
        editor-cb (fn [md plain editor-state]
                    (let [new-state (js->clj editor-state :keywordize-keys true)
                          updated (merge
                                    @entry
                                    (p/parse-entry md)
                                    {:editor-state new-state
                                     :text         plain})]
                      (put-fn [:entry/update-local updated])))]
    (fn [entry put-fn]
      (let [latest-entry (dissoc @entry :comments)
            save-fn (fn [_ev]
                      (put-fn
                        [:entry/update
                         (if (and (:new-entry entry) (not (:comment-for entry)))
                           (update-in (u/clean-entry latest-entry) [:tags] conj "#new")
                           (u/clean-entry latest-entry))]))
            md (or (:md @entry) "")]
        [:div
         [draft-text-editor editor-state md editor-cb]
         [:div.save
          (when
            (not= (:editor-state @local)
                  (:editor-state @entry))
            [:span.not-saved {:on-click save-fn}
             [:span.fa.fa-floppy-o] "  click to save"])]]))))

(defn story-search-field
  [editor-state select-story]
  (let [options (subscribe [:options])
        sorted-stories (reaction (:sorted-stories @options))
        editor (adapt-react-class "StoryFieldEditor")]
    (fn [editor-state select-story]
      (let [stories-list (map story-mapper @sorted-stories)]
        [editor {:editorState editor-state
                 :stories     stories-list
                 :selectStory select-story}]))))

(ns iwaswhere-web.ui.draft
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.core :as r]))

(defn editor-state-from-text
  [text]
  (let [content-from-text (.createFromText js/Draft.ContentState text)]
    (.createWithContent js/Draft.EditorState content-from-text)))

(defn editor-state-from-raw
  [editor-state]
  (let [content-from-raw (.convertFromRaw js/Draft editor-state)]
    (.createWithContent js/Draft.EditorState content-from-raw)))

(defn draft-search-field
  [editor-state update-cb mentions hashtags]
  (let [editor (r/adapt-react-class
                 (aget js/window "deps" "SearchFieldEditor" "default"))
        on-change (fn [new-state]
                    (let [current-content (.getCurrentContent new-state)
                          plain (.getPlainText current-content)
                          raw-content (.convertToRaw js/Draft current-content)
                          via-json (.parse js/JSON (.stringify js/JSON raw-content))
                          new-state (js->clj via-json :keywordize-keys true)]
                      (update-cb plain new-state)))]
    (fn [editor-state send-fn mentions hashtags]
      [editor {:editorState editor-state
               :spellCheck  true
               :mentions    mentions
               :hashtags    hashtags
               :onChange    on-change}])))

(defn story-search-field
  [editor-state select-story _stories]
  (let [editor (r/adapt-react-class
                 (aget js/window "deps" "StoryFieldEditor" "default"))]
    (fn [editor-state select-story stories]
      [editor {:editorState editor-state
               :stories     stories
               :selectStory select-story}])))

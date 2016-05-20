(ns iwaswhere-web.ui.edit
  "This namespace holds the fucntions for rendering the text (markdown) content of a journal entry.
  This includes both a properly styled element for static content and the edit-mode view, with
  autosuggestions for tags and mentions."
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.utils :as u]
            [reagent.core :as r]
            [clojure.string :as s]))

(defn suggestions
  "Renders suggestions for hashtags or mentions if either occurs before the current caret position.
  It does so by getting the selection from the DOM API, which can be used to determine the position
  and a string before that position, then finding either a hashtag or mention fragment right at the
  and of that substring. For these, auto-suggestions are displayed, which are entities that begin
  with the tag fragment before the caret position. When any of the suggestions are clicked, the
  fragment will be replaced with the clicked item."
  [entry filtered-tags current-tag tag-replace-fn css-class]
  (let [ts (:ts entry)]
    [:div.suggestions
     (for [tag filtered-tags]
       ^{:key (str ts tag)}
       [:div {:on-click #(tag-replace-fn current-tag tag)}
        [:span {:class css-class} tag]])]))

(defn editable-code-elem
  "Code element, with content editable. Takes md-string to render, update-temp-fn which is
  called with any input to the element, and the on-keydown-fn, which is called for each keystroke
  and can be used for intercepting key combinations such as CMD-s for saving the content.
  Also takes an atom which it will reset to the actual DOM element. This can then be used for focusing
  on the element."
  [md-string update-temp-fn on-keydown-fn edit-elem-atom]
  (r/create-class
    {:component-did-mount #(let [el (r/dom-node %)]
                            (reset! edit-elem-atom el)
                            (u/focus-on-end el))
     :reagent-render      (fn [md-string update-temp-fn on-keydown-fn edit-elem-atom]
                            [:code {:content-editable true
                                    :on-input         update-temp-fn
                                    :on-key-down      on-keydown-fn}
                             md-string])}))

(defn autocomplete-tags
  "Render autocomplete option for the partial tag (or mention) before the cursor."
  [before-cursor regex-prefix tags]
  (let [current-tag (re-find (js/RegExp. (str regex-prefix h/tag-char-class "+$") "m") before-cursor)
        current-tag-regex (js/RegExp. current-tag "i")
        tag-substr-filter (fn [tag] (when current-tag (re-find current-tag-regex tag)))
        f-tags (filter tag-substr-filter tags)]
    [current-tag f-tags]))

(defn editable-md-render
  "Renders markdown in a pre>code element, with editable content. Sends update message to store
  component on any change to the component. The save button sends updated entry to the backend.
  Maintains some local state for storing changes before they are persisted in the backend.
  Keeps track of current cursor position and potential incomplete tags or mentions before the
  cursor, which can then be completed by clicking on an empty in the autosuggested list, or by
  using the tab key for selecting the first one."
  [entry hashtags mentions put-fn toggle-edit new-entry?]
  (let [entry (dissoc entry :comments)
        edit-elem-atom (atom {})
        last-saved (r/atom entry)
        local-saved-entry (r/atom entry)
        local-display-entry (r/atom entry)]
    (fn [entry hashtags mentions put-fn toggle-edit new-entry?]
      (let [latest-entry (dissoc entry :comments)
            md-string (or (:md @local-display-entry) "edit here")
            get-content #(aget (.. % -target -parentElement -parentElement -firstChild -firstChild) "innerText")
            update-temp-fn #(let [updated-entry (merge latest-entry (h/parse-entry (get-content %)))]
                             (put-fn [:entry/update-local updated-entry])
                             (reset! local-saved-entry updated-entry))
            save-fn #(put-fn [:text-entry/update @local-saved-entry])

            ;determine cursor position
            selection (.getSelection js/window)
            cursor-pos (.-anchorOffset selection)
            anchor-node (aget selection "anchorNode")
            node-value (str (when anchor-node (aget anchor-node "nodeValue")) "")
            before-cursor (if (not= -1 (.indexOf (:md @local-saved-entry) node-value))
                            (subs node-value 0 cursor-pos)
                            "")
            ; find incomplete tag and mention before cursor
            [curr-tag f-tags] (autocomplete-tags before-cursor "(?!^)#" hashtags)
            [curr-mention f-mentions] (autocomplete-tags before-cursor "@" mentions)

            tag-replace-fn (fn [curr-tag tag]
                             (let [curr-tag-regex (js/RegExp (str curr-tag "(?!" h/tag-char-class ")") "i")
                                   md (:md @local-saved-entry)
                                   updated (merge entry (h/parse-entry (s/replace md curr-tag-regex tag)))]
                               (reset! local-saved-entry updated)
                               (reset! local-display-entry updated)
                               (.setTimeout js/window (fn [] (u/focus-on-end @edit-elem-atom)) 100)))

            on-keydown-fn (fn [ev]
                            (let [key-code (.. ev -keyCode)
                                  meta-key (.. ev -metaKey)]
                              (when (and meta-key (= key-code 83))
                                (if (or new-entry? (not= @last-saved @local-saved-entry))
                                  (save-fn)
                                  (toggle-edit)) ; when no change, toggle edit mode
                                (.preventDefault ev))
                              (when (= key-code 9)          ; TAB key pressed
                                (when (and curr-tag (seq f-tags))
                                  (tag-replace-fn curr-tag (first f-tags)))
                                (when (and curr-mention (seq f-mentions))
                                  (tag-replace-fn curr-mention (first f-mentions)))
                                (.preventDefault ev))))]
        (when (and (not new-entry?) (not= @last-saved latest-entry))
          (reset! last-saved latest-entry)
          (reset! local-display-entry latest-entry)
          (reset! local-saved-entry latest-entry))
        [:div.edit-md
         [:pre [editable-code-elem md-string update-temp-fn on-keydown-fn edit-elem-atom]]
         [suggestions entry f-tags curr-tag tag-replace-fn "hashtag"]
         [suggestions entry f-mentions curr-mention tag-replace-fn "mention"]
         (when (or (not= @last-saved @local-saved-entry) new-entry?)
           [:span.not-saved {:on-click save-fn} [:span.fa.fa-floppy-o] "  click to save"])]))))

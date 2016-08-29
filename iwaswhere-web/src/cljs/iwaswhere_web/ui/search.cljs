(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [clojure.string :as s]
            [clojure.set :as set]
            [reagent.core :as r]))

(defn tags-view
  "Renders a row with tags, if any in current query."
  [current-query]
  (let [get-tags #(% current-query)
        tags (get-tags :tags)
        not-tags (get-tags :not-tags)
        mentions (get-tags :mentions)]
    (when (or (seq tags) (seq not-tags) (seq mentions))
      [:div.hashtags
       (for [tag tags]
         ^{:key (str "search-" tag)}
         [:span.hashtag tag])
       (for [tag not-tags]
         ^{:key (str "search-n" tag)}
         [:span.hashtag.not-tag tag])
       (for [tag mentions]
         ^{:key (str "search-" tag)}
         [:span.mention tag])])))

(defn search-field-view
  "Renders search field for current tab."
  [snapshot put-fn query-id]
  (let [focused (r/atom false)]
    (fn [snapshot put-fn query-id]
      (let [current-query (query-id (:queries (:query-cfg snapshot)))
            update-search-fn (fn [search-str]
                               (put-fn [:search/update
                                        (merge {:query-id query-id}
                                               (p/parse-search search-str))]))
            before-cursor (h/string-before-cursor (:search-text current-query))
            cfg (:cfg snapshot)
            options (:options snapshot)
            show-pvt? (:show-pvt cfg)
            hashtags (:hashtags options)
            pvt-hashtags (:pvt-hashtags options)
            hashtags (if show-pvt? (set/union hashtags pvt-hashtags) hashtags)
            mentions (:mentions options)
            [curr-tag f-tags] (p/autocomplete-tags before-cursor "#" hashtags)
            [curr-mention f-mentions] (p/autocomplete-tags before-cursor "@" mentions)
            on-input-fn (fn [ev] (update-search-fn (aget ev "target" "innerText")))
            tag-replace-fn
            (fn [curr-tag tag]
              (let [curr-regex (js/RegExp (str curr-tag "(?!" p/tag-char-cls ")") "i")
                    search-text (:search-text current-query)]
                (update-search-fn (s/replace search-text curr-regex tag))))
            on-keydown-fn
            (fn [ev]
              (let [key-code (.. ev -keyCode)]
                (when (= key-code 9)                        ; TAB key pressed
                  (when (and curr-tag (seq f-tags))
                    (tag-replace-fn curr-tag (first f-tags)))
                  (when (and curr-mention (seq f-mentions))
                    (tag-replace-fn curr-mention (first f-mentions)))
                  ;(.setTimeout js/window (fn [] (h/focus-on-end (.-target ev))) 50)
                  (.preventDefault ev))))]
        (prn "focused" @focused)
        [:div.search
         [tags-view current-query]
         [:div.search-field {:content-editable true
                             :on-input         on-input-fn
                             :on-key-down      on-keydown-fn
                             :on-focus         #(reset! focused true)
                             :on-blur          #(reset! focused false)}
          (:search-text current-query)]
         (when @focused
           [u/suggestions
            "search" f-tags curr-tag tag-replace-fn "hashtag"])
         (when @focused
           [u/suggestions
            "search" f-mentions curr-mention tag-replace-fn "mention"])]))))

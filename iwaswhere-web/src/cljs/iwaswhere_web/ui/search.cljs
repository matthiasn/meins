(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [clojure.string :as s]
            [clojure.set :as set]
            [reagent.core :as rc]))

(defn search-field-view
  [snapshot put-fn query-id]
  (let [local (rc/atom {})]
    (fn [snapshot put-fn]
      (let [get-tags #(% (:current-query @local))
            local-snapshot @local
            before-cursor (h/string-before-cursor
                            (:search-text (:current-query @local)))
            show-pvt? (:show-pvt (:cfg snapshot))
            hashtags (:hashtags (:cfg snapshot))
            pvt-hashtags (:pvt-hashtags (:cfg snapshot))
            hashtags (if show-pvt? (set/union hashtags pvt-hashtags) hashtags)
            mentions (:mentions (:cfg snapshot))
            [curr-tag f-tags] (p/autocomplete-tags before-cursor "#" hashtags)
            [curr-mention f-mentions] (p/autocomplete-tags before-cursor "@" mentions)
            on-input-fn (fn [ev]
                          (let [search (p/parse-search
                                         (aget ev "target" "innerText"))]
                            (swap! local assoc-in [:current-query] search)
                            (put-fn [:search/update (merge {:query-id query-id}
                                                           search)])))
            tag-replace-fn
            (fn [curr-tag tag]
              (let [curr-tag-regex (js/RegExp (str curr-tag "(?!" p/tag-char-cls ")") "i")
                    search-text (:search-text (:current-query @local))
                    new-search (p/parse-search (s/replace search-text curr-tag-regex tag))]
                (swap! local assoc-in [:current-query] new-search)
                (put-fn [:search/update (merge {:query-id query-id}
                                               new-search)])))
            on-keydown-fn
            (fn [ev]
              (let [key-code (.. ev -keyCode)]
                (when (= key-code 9)          ; TAB key pressed
                  (when (and curr-tag (seq f-tags))
                    (tag-replace-fn curr-tag (first f-tags)))
                  (when (and curr-mention (seq f-mentions))
                    (tag-replace-fn curr-mention (first f-mentions)))
                  ;(.setTimeout js/window (fn [] (h/focus-on-end (.-target ev))) 50)
                  (.preventDefault ev))))]
        [:div.search
         [:div.hashtags
          (for [tag (get-tags :tags)]
            ^{:key (str "search-" tag)}
            [:span.hashtag tag])
          (for [tag (get-tags :not-tags)]
            ^{:key (str "search-n" tag)}
            [:span.hashtag.not-tag tag])
          (for [tag (get-tags :mentions)]
            ^{:key (str "search-" tag)}
            [:span.mention tag])]
         [:div.search-field {:content-editable true
                             :on-input         on-input-fn
                             :on-key-down      on-keydown-fn}
          (:search-text (:current-query local-snapshot))]
         [u/suggestions "search" f-tags curr-tag tag-replace-fn "hashtag"]
         [u/suggestions "search" f-mentions curr-mention tag-replace-fn "mention"]]))))

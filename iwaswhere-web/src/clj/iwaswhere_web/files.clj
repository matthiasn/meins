(ns iwaswhere-web.files)

(defn filter-by-name
  "Filter a sequence of files by their name, matched via regular expression."
  [file-s regexp]
  (filter (fn [f] (re-matches regexp (.getName f))) file-s))

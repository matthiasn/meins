(ns iwaswhere-web.imports
  "This namespace does imports, for example of photos."
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.files :as f])
  (:import [com.drew.imaging ImageMetadataReader]))

(defn dms-to-dd
  "Converts DMS (degree, minute, second) to DD (decimal degree) format. Returns nil
  when not all 3 groups dm, m, and s are contained in coord string. Result negative
  when coord in Western or Southern Hemisphere according to ref argument."
  [coord ref]
  (let [matcher (re-matcher #"(\d{1,3})° (\d{1,2})' (\d{1,2}\.?\d+?)" coord)
        [_dms d m s] (re-find matcher)]
    (when (and d m s)
      (let [d (read-string d)
            m (read-string m)
            s (read-string s)
            dd (float (+ d (/ m 60) (/ s 3600)))]
        (if (contains? #{"W" "S"} ref)
          (- dd)
          dd)))))

(defn extract-from-tag [tag] (into {} (map #(hash-map (.getTagName %) (.getDescription %)) tag)))

(defn exif-for-file
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map.
  Borrowed and modified from: https://github.com/joshuamiller/exif-processor (including extract-from-tag fn)"
  [file]
  (let [metadata (ImageMetadataReader/readMetadata file)
        exif-directories (.getDirectories metadata)
        tags (map #(.getTags %) exif-directories)
        raw-exif (into {} (map extract-from-tag tags))
        lat-dms (get raw-exif "GPS Latitude")
        lat-ref (get raw-exif "GPS Latitude Ref")
        lon-dms (get raw-exif "GPS Longitude")
        lon-ref (get raw-exif "GPS Longitude Ref")]
        {:raw-exif raw-exif
         :latitude (dms-to-dd lat-dms lat-ref)
         :longitude (dms-to-dd lon-dms lon-ref)}))

(defn import-photos
  "Imports photos from respective directory."
  [{:keys [current-state]}]
    (let [files (file-seq (clojure.java.io/file "data/image-import"))]
      (doseq [img (f/filter-by-name files #"[A-Za-z0-9_]+.jpg")]
        (prn (.getName img))
        (prn (.getPath img))
        (prn (.getAbsolutePath img))
        (spit "xxxxx.jpg" img)
        (pp/pprint
          (exif-for-file img)))
      #_
      {:new-state new-state
       :emit-msg  [:state/new new-state]}))

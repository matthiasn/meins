(ns iwaswhere-web.imports
  "This namespace does imports, for example of photos."
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.files :as f]
            [clj-time.coerce :as c]
            [cheshire.core :as cc]
            [matthiasn.systems-toolbox.component :as st]
            [me.raynes.fs :as fs]
            [clj-time.format :as tf]
            [clojure.tools.logging :as log])
  (:import [com.drew.imaging ImageMetadataReader]
           (java.io BufferedReader)))

(defn dms-to-dd
  "Converts DMS (degree, minute, second) to DD (decimal degree) format. Returns nil
  when not all 3 groups dm, m, and s are contained in coord string. Result negative
  when coord in Western or Southern Hemisphere according to ref argument."
  [coord ref]
  (when coord
    (let [matcher (re-matcher #"(\d{1,3})° (\d{1,2})' (\d{1,2}\.?\d+?)" coord)
          [_dms d m s] (re-find matcher)]
      (when (and d m s)
        (let [d (read-string d)
              m (read-string m)
              s (read-string s)
              dd (float (+ d (/ m 60) (/ s 3600)))]
          (if (contains? #{"W" "S"} ref)
            (- dd)
            dd))))))

(defn extract-ts
  "Converts concatenated GPS timestamp strings into milliseconds since epoch.
  Example from iPhone 6s camera app:
    'GPS Date Stamp' '2016:03:30'
    'GPS Time-Stamp' '20:07:57.00 UTC'"
  [ts]
  (let [f (tf/formatter "yyyy:MM:dd HH:mm:ss.SS z")]
    (when (and (seq ts)
               (re-matches #"\d{4}:\d{2}:\d{2} \d{2}:\d{2}:\d{2}\.\d{2} [A-Z]{1,5}" ts))
      (c/to-long (tf/parse f ts)))))

(defn extract-from-tag
  "Creates map for a single Exif directory.
  Borrowed from: https://github.com/joshuamiller/exif-processor"
  [tag]
  (into {} (map #(hash-map (.getTagName %) (.getDescription %)) tag)))

(defn exif-for-file
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map.
  Borrowed and modified from: https://github.com/joshuamiller/exif-processor"
  [file]
  (let [metadata (ImageMetadataReader/readMetadata file)
        exif-directories (.getDirectories metadata)
        tags (map #(.getTags %) exif-directories)
        raw-exif (into {} (map extract-from-tag tags))
        lat-dms (get raw-exif "GPS Latitude")
        lat-ref (get raw-exif "GPS Latitude Ref")
        lon-dms (get raw-exif "GPS Longitude")
        lon-ref (get raw-exif "GPS Longitude Ref")
        gps-date (get raw-exif "GPS Date Stamp")
        gps-time (get raw-exif "GPS Time-Stamp")]
    {:raw-exif  raw-exif
     :timestamp (or (extract-ts (str gps-date " " gps-time))
                    (extract-ts (str (get raw-exif "Date/Time") ".00 UTC"))
                    (extract-ts (str (get raw-exif "Date/Time Original") ".00 UTC"))
                    (st/now))
     :latitude  (dms-to-dd lat-dms lat-ref)
     :longitude (dms-to-dd lon-dms lon-ref)}))

(defn import-photos
  "Imports photos from respective directory."
  [{:keys [put-fn msg-meta]}]
  (let [files (file-seq (clojure.java.io/file "data/image-import"))]
    (log/info "importing photos")
    (doseq [img (f/filter-by-name files #"[A-Za-z0-9_]+.(jpg|JPG|PNG|png)")]
      (let [filename (.getName img)]
        (log/info "Trying to import " filename)
        (try (let [rel-path (.getPath img)
                   file-info (exif-for-file img)
                   target-filename (str (:timestamp file-info) "-" filename)
                   new-entry (merge file-info
                                    {:img-file target-filename
                                     :tags     #{"#photo"}})]
               (fs/rename rel-path (str "data/images/" target-filename))
               (put-fn (with-meta [:geo-entry/persist new-entry] msg-meta)))
             (catch Exception ex (log/error (str "Error while importing " filename) ex)))))))

(defn import-geo
  "Imports geo data from respective directory.
  For now, only pays attention to visits."
  [{:keys [put-fn msg-meta]}]
  (let [files (file-seq (clojure.java.io/file "data/geo-import"))]
    (log/info "importing photos")
    (doseq [file (f/filter-by-name files #"visits.json" #_#"[-0-9]+.(json)")]
      (let [filename (.getName file)]
        (log/info "Trying to import " filename)
        (try (let [lines (line-seq (clojure.java.io/reader file))]
               (doseq [line lines]
                 (let [raw-visit (cc/parse-string line true)
                       arrival-ts (:arrival-timestamp raw-visit)
                       departure-ts (:departure-timestamp raw-visit)
                       dur (-> (- departure-ts arrival-ts)
                                    (/ 6000)
                                    (Math/floor)
                                    (/ 10))
                       visit (merge raw-visit {:timestamp arrival-ts
                                               :md        (if (> dur 9999)
                                                            "No departure recorded #visit"
                                                            (str "Duration: " dur "m #visit"))
                                               :tags #{"#visit"}})]
                   (put-fn (with-meta [:geo-entry/persist visit] msg-meta)))))
             (catch Exception ex (log/error (str "Error while importing " filename) ex)))))))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:import/photos import-photos
                 :import/geo    import-geo}})

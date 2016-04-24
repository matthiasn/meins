(ns iwaswhere-web.imports
  "This namespace does imports, for example of photos."
  (:require [clojure.pprint :as pp]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.migrations :as m]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [cheshire.core :as cc]
            [matthiasn.systems-toolbox.component :as st]
            [me.raynes.fs :as fs]
            [clj-time.format :as tf]
            [clojure.tools.logging :as log]
            [clojure.string :as s])
  (:import [com.drew.imaging ImageMetadataReader]
           (java.io BufferedReader)))

(defn dms-to-dd
  "Converts DMS (degree, minute, second) to DD (decimal degree) format. Returns nil
  when not all 3 groups dm, m, and s are contained in coord string. Result negative
  when coord in Western or Southern Hemisphere according to ref argument."
  [exif coord-key ref-key]
  (let [coord (get exif coord-key)
        ref (get exif ref-key)]
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
              dd)))))))

(defn add-filename-offset
  "Add the last three digits of the filename to the timestamp in order to avoid collisions when
  photos were taken in burst mode on the iPhone, as unfortunately the photo timestamp is only
  precise to the second and the capture rate for iPhone 6s is 10fps."
  [millis filename]
  (let [filename-number (re-find #"\d{3}(?=\.)" filename)
        offset (if filename-number (Integer. filename-number) 0)]
    (+ millis offset)))

(defn extract-gps-ts
  "Converts concatenated GPS timestamp strings into milliseconds since epoch.
  Example from iPhone 6s camera app:
    'GPS Date Stamp' '2016:03:30'
    'GPS Time-Stamp' '20:07:57.00 UTC'"
  [ts filename]
  (let [f (tf/formatter "yyyy:MM:dd HH:mm:ss.SS z")]
    (when (and (seq ts)
               (re-matches #"\d{4}:\d{2}:\d{2} \d{2}:\d{2}:\d{2}\.\d{2} [A-Z]{1,5}" ts))
      (add-filename-offset (c/to-long (tf/parse f ts)) filename))))

(defn extract-ts
  "Converts 'Date/Time' exif tag into milliseconds since epoch. Assumes local timezone at the time
  of importing a photo, which may or may not be accurate. In case of iPhone, this is only relevant
  for screenshots, media saved to local camera roll or when no GPS available, such as in the case
  of being in an aircraft (airplane mode). This is unfortunate, why would they not just use UTC
  all the time?"
  [ts filename]
  (let [dtz (t/default-time-zone)
        f (tf/formatter "yyyy:MM:dd HH:mm:ss" dtz)]
    (when (and (seq ts)
               (re-matches #"\d{4}:\d{2}:\d{2} \d{2}:\d{2}:\d{2}" ts))
      (add-filename-offset (c/to-long (tf/parse f ts)) filename))))

(defn extract-from-tag
  "Creates map for a single Exif directory.
  Borrowed from: https://github.com/joshuamiller/exif-processor"
  [tag]
  (into {} (map #(hash-map (.getTagName %) (.getDescription %)) tag)))

(defn exif-for-file
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map.
  Borrowed and modified from: https://github.com/joshuamiller/exif-processor"
  [file]
  (let [filename (.getName file)
        metadata (ImageMetadataReader/readMetadata file)
        exif-directories (.getDirectories metadata)
        tags (map #(.getTags %) exif-directories)
        exif (into {} (map extract-from-tag tags))]
    {:raw-exif  exif
     :timestamp (or (extract-gps-ts (str (get exif "GPS Date Stamp") " " (get exif "GPS Time-Stamp")) filename)
                    (extract-ts (str (get exif "Date/Time")) filename)
                    (extract-ts (str (get exif "Date/Time Original")) filename)
                    (st/now))
     :latitude  (dms-to-dd exif "GPS Latitude" "GPS Latitude Ref")
     :longitude (dms-to-dd exif "GPS Longitude" "GPS Longitude Ref")}))

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
                   new-entry (merge file-info {:img-file target-filename
                                               :tags     #{"#photo" "#import"}})]
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
                 (let [raw-visit (cc/parse-string line #(keyword (s/replace % "_" "-")))
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
                                               :tags #{"#visit" "#import"}})]
                   (put-fn (with-meta [:geo-entry/persist visit] msg-meta)))))
             (catch Exception ex (log/error (str "Error while importing " filename) ex)))))))

(defn import-phone
  "Imports text entries from phone."
  [{:keys [put-fn msg-meta]}]
  (let [files (file-seq (clojure.java.io/file "data/geo-import"))]
    (log/info "importing photos")
    (doseq [file (f/filter-by-name files #"text-entries.json")]
      (let [filename (.getName file)]
        (log/info "Trying to import " filename)
        (try (let [lines (line-seq (clojure.java.io/reader file))]
               (doseq [line lines]
                 (let [entry (m/add-tags-mentions (cc/parse-string line #(keyword (s/replace % "_" "-"))))]
                   (put-fn (with-meta [:geo-entry/persist entry] msg-meta)))))
             (catch Exception ex (log/error (str "Error while importing " filename) ex)))))))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:import/photos import-photos
                 :import/geo    import-geo
                 :import/phone  import-phone}})

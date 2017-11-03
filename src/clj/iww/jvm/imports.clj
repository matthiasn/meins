(ns iww.jvm.imports
  (:require [iww.jvm.imports.screenshot :as is]
            [iww.jvm.imports.spotify :as iss]
            [iww.jvm.imports.entries :as ie]
            [iww.jvm.imports.flight :as fl]
            [iww.jvm.imports.media :as im]))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:import/photos     im/import-media
                 :import/screenshot is/import-screenshot
                 :import/movie      im/import-movie
                 :import/spotify    iss/import-spotify
                 :import/flight     fl/import-flight}})

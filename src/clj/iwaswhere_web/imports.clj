(ns iwaswhere-web.imports
  (:require [iwaswhere-web.imports.screenshot :as is]
            [iwaswhere-web.imports.spotify :as iss]
            [iwaswhere-web.imports.entries :as ie]
            [iwaswhere-web.imports.flight :as fl]
            [iwaswhere-web.imports.media :as im]))

(defn cmp-map
  "Generates component map for imports-cmp."
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:import/photos     im/import-media
                 :import/screenshot is/import-screenshot
                 :import/geo        ie/import-geo
                 :import/movie      im/import-movie
                 :import/spotify    iss/import-spotify
                 :import/flight     fl/import-flight
                 :import/phone      ie/import-text-entries}})

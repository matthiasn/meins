(ns iwaswhere-web.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [iwaswhere-web.client-store-test]
            [iwaswhere-web.client-store-entry-test]
            [iwaswhere-web.client-store-search-test]
            [iwaswhere-web.client-keepalive-test]))

(doo-tests 'iwaswhere-web.client-store-test
           'iwaswhere-web.client-store-entry-test
           'iwaswhere-web.client-store-search-test
           'iwaswhere-web.client-keepalive-test)

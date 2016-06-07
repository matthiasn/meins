(ns iwaswhere-web.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [iwaswhere-web.client-store-test]
            [iwaswhere-web.client-store-entry-test]
            [iwaswhere-web.client-store-search-test]
            [iwaswhere-web.client-keepalive-test]
            [iwaswhere-web.ui-markdown-test]
            [iwaswhere-web.ui-pomodoros-test]
            [iwaswhere-web.ui-utils-test]))

(doo-tests 'iwaswhere-web.client-store-test
           'iwaswhere-web.client-store-entry-test
           'iwaswhere-web.client-store-search-test
           'iwaswhere-web.client-keepalive-test
           'iwaswhere-web.ui-markdown-test
           'iwaswhere-web.ui-pomodoros-test
           'iwaswhere-web.ui-utils-test)

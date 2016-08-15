# iwaswhere-web

This is the **web application** part of **iWasWhere**. You can read more about the motivation **[here](../README.md)**. This system is written in **Clojure** and **ClojureScript**, making use of the **[systems-toolbox](https://github.com/matthiasn/systems-toolbox)** for its architecture.


## Installation

First, you need to compile the **ClojureScript**:

    $ lein cljsbuild once release

This will compile the ClojureScript into JavaScript using `:advanced` optimization.
    
The styling is maintained via **SASS** in a bunch of `.scss` files. These need to be converted to CSS:

    $ lein sass once

There is no **CSS** framework involved here. Rather, the styling is self-contained, which is possible thanks to **[CSS Flexible Box Layout](https://www.w3.org/TR/css-flexbox-1/)**. It's great for layout. You should learn it.

You can also use the following task bundle instead of the previous two:

    $ lein build

## Usage

Once you have completed all the steps in the previous section, all you need to do is:

    $ lein run

This will run the application on **[http://localhost:8888/](http://localhost:8888/)**. By default, the webserver exposed by the systems-toolbox library listens on port 8888 and only binds to the localhost interface. You can use environment variables to change this behavior, for example:

    $ HOST="0.0.0.0" PORT=8888 lein run


## Development

During development, it makes sense to automatically recompile the **ClojureScript** when any change is detected. You can use **cljsbuild**, which works very well, but requires reloading the web page:

    $ lein cljsbuild auto release

Alternatively, you can use **[Figwheel](https://github.com/bhauman/lein-figwheel)** to automatically update the application as you make changes. For that, open another terminal:

    $ lein figwheel

The **systems-toolbox** library supports **Figwheel**, and all components will be reloaded while retaining their previous state. This works particularly well when doing changes that don't affect component state structure. Figwheel also detects CSS changes. Here, it particularly shines, as the UI will re-render immediately after any CSS change, without jumping or going back to the initial state after loading. For this, you need to keep the **sass watch** task running, like so:

    $ lein sass watch


## Tests

There a **unit-tests** for the handler functions on the server side. It's useful to start them in refresh mode so that they get run again on each code change:

    $ lein test-refresh

This mechanism is also very fast. While the initial startup takes like 10 seconds, each subsequent run currently takes around 100ms. If you have **[growl](http://growl.info/)** installed, you can also call it like this:

    $ lein test-refresh :growl

You can also run the platform-independent tests in the browser using `lein doo`, like this:

    $ lein doo firefox cljs-test once
    $ lein doo firefox cljs-test auto


## REPL

Inspecting the store component:

    (use 'iwaswhere-web.core)
    (restart! switchboard)
    (def store (:cmp-state (:server/store-cmp (:components @(:cmp-state switchboard)))))
    (def g (:graph @store))
    
    (require '[ubergraph.core :as uber])
    (uber/has-node? g :hashtags)
    
    ; find all hashtags
    (def hashtags (map #(-> % :dest :tag) (uber/find-edges g {:src :hashtags})))
    
    (pprint (sort (map clojure.string/lower-case hashtags)))



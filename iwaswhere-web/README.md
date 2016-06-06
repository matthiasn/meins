# iWasWhere

This is the web and backend portion of a system for tracking the user's movement in space, plus related tasks and ideas.

## Usage

Before first usage, you want to install the **[Bower](http://bower.io)** dependencies:

    $ bower install

Once this is done, you can start the application as usual:

    $ lein run

This will run the application on **[http://localhost:8888/](http://localhost:8888/)**. However, we will still need to compile the ClojureScript:

    $ lein cljsbuild auto release

This will compile the ClojureScript into JavaScript using `:advanced` optimization.

You can also use **[Figwheel](https://github.com/bhauman/lein-figwheel)** to automatically update the application as you make changes. For that, open another terminal:

    $ lein figwheel

By default, the webserver exposed by the systems-toolbox library listens on port 8888 and only binds to the localhost interface. You can use environment variables to change this behavior, for example:

    $ HOST="0.0.0.0" PORT=8888 lein run
    
The styling is maintained via SASS in a bunch of `.scss` files. Automatic conversion to CSS can be started with:

    $ lein sass watch


## Tests

There a **unit-tests** for the handler functions on the server side. It's useful to start them in refresh mode so that they get run again on each code change:

    $ lein test-refresh

This mechanism is also very fast. While the initial startup takes like 10 seconds, each subsequent run currently takes around 100ms. If you have **[growl](http://growl.info/)** installed, you can also call it like this:

    $ lein test-refresh :growl


## REPL

Inspecting the store component:

    (use 'iwaswhere-web.core)
    (restart!)
    (def store (:cmp-state (:server/store-cmp (:components @(:cmp-state switchboard)))))
    (def g (:graph @store))
    
    (require '[ubergraph.core :as uber])
    (uber/has-node? g :hashtags)
    
    ; find all hashtags
    (def hashtags (map #(-> % :dest :tag) (uber/find-edges g {:src :hashtags})))
    
    (pprint (sort (map clojure.string/lower-case hashtags)))



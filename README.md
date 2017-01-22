# iWasWhere

**iWasWhere** is a **personal information manager**, with a **geo-aware diary** or **log** and **task tracking**. It allows me to make sense of where I was and what I did there.


## Motivation

Over the course of the past twenty years, I've traveled the globe a lot, usually multiple times every year. But do I know where I was exactly 10 or 15 years ago? Oh no, even one year ago I have no clue. I have vague ideas about the places I have visited, but I probably won't even be able to pinpoint the exact year. That's not enough. See, life won't go well forever. There absolutely will come a time when I won't be able to travel nearly as much. Would I then want to settle with faded memories of the past? No, most certainly not.

I recently had tea with my 92-year-old grandma, and we were looking at her collection of photographs. I later noticed that she had a handwritten list of the places she had been to, with the exact dates, who with, and some brief notes about the occasion. I wish I was also keeping records of every aspect of my life, but unfortunately my handwriting is terrible so it would have to be done electronically. 

Then I decided to build an application that will provide me with the tools to plan (and record) my life better. It should allow me to write down every thought, note, photo, video or whatnot while keeping track of the location. In addition, I should be able to **retrieve information** so that I can always find anything later on. Since there will be a lot of private information that will accumulate over time, the data should not be stored in the cloud but rather locally.

**iWasWhere** is what I came up with as a solution. I use it every day and I have so far recorded over **4,200** entries, in about 10 weeks. It is also a suitable sample application for my book **[Building a System in Clojure](https://leanpub.com/building-a-system-in-clojure)**. As a bonus, it could also be useful for anyone who would like to keep a record of their thoughts, ideas, and projects, all while recording the exact whereabouts on what took place and where it happened.


## Components

**iWasWhere** consists of a **[Clojure](https://clojure.org/)** and **[ClojureScript](https://github.com/clojure/clojurescript)** system spanning the **browser** and a backend that runs on the **[JVM](https://en.wikipedia.org/wiki/Java_virtual_machine)**. This project lives in the **[iwaswhere-web](https://github.com/matthiasn/iWasWhere/tree/master/iwaswhere-web)** directory, which is also where you can find installation instructions and more information about the system's architecture.

There's also an **iOS app** that keeps track of visits and lets me quickly capture thoughts on the go. Currently, it's a very basic application written in **[Swift](https://swift.org/)**. It is not open sourced yet, but that will happen in due course. Ideally, by then it should also have been rewritten in **ClojureScript** and **[React Native](https://facebook.github.io/react-native/)**. Stay tuned.

This repository contains the **web application** part of **iWasWhere**. This system is written in **Clojure** and **ClojureScript**, making use of the **[systems-toolbox](https://github.com/matthiasn/systems-toolbox)** for its architecture.


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


[![CircleCI Build Status](https://circleci.com/gh/matthiasn/iWasWhere.svg?&style=shield)](https://circleci.com/gh/matthiasn/iWasWhere)
[![TravisCI Build Status](https://travis-ci.org/matthiasn/iWasWhere.svg?branch=master)](https://travis-ci.org/matthiasn/iWasWhere)


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


## License

Copyright © 2016 **[Matthias Nehlsen](http://www.matthiasnehlsen.com)**. Distributed under the **GNU AFFERO PUBLIC LICENSE**, Version 3. See separate LICENSE file.

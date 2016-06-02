# iWasWhere

**iWasWhere** is a **personal information manager**, with a **geo-aware diary or log** and **task tracking**. It allows me to make sense of where I was and what I did there.


## Motivation

Over the course of the past twenty years, I've traveled the globe a lot, usually multiple times every year. But do I know where I was exactly 10 or 15 years ago? Oh no, even one year ago I have no clue. I have vague ideas about the places I have visited, but I probably won't even be able to pinpoint the year. That's not enough. See, life won't go well forever. There absolutely will come a time when I won't be able to travel near as much. Would I then want to settle with faded memories of the past? Most certainly not.

I recently had tea with my 92-year-old grandma, and we were looking at her collection of photographs. Then, I noticed she had a handwritten list of the places she had been to, with the exact dates, who with, and some brief notes about the occasion. I was I had something like that for those past 20 years mentioned earlier. But then again, my handwriting is terrible, so I probably wouldn't be able to decipher that list anyway. Rather, I wanted a decent repository for such information, with the location of every thought, note, photo, video or whatnot. Ideally, this system should also let me track my **goals** and **tasks** and provide the tools to plan my life better. All that of course with **information retrieval** in mind so that I can always find anything later on. Of course, a lot of private information would accumulate in such a system, so the data could not be stored in the cloud but rather locally.

I'm also writing this book about **[Building a System in Clojure](https://leanpub.com/building-a-system-in-clojure)**, and the writing process has stalled for multiple reasons. I needed a sample application to write about, one that I actively develop and that I use myself. Then, I also didn't have a good way to support me in planning my days and achieve a long-running goal such as finishing a book. This application solves all these problems. I now have a good way to track progress, and at the same time, I think this system will also be a suitable sample application for the book. As a bonus, it could also be useful for other people in tracking their thoughts, ideas, and projects, all while recording the exact whereabouts on what took place where.


## Components

**iWasWhere** consists of a **[Clojure](https://clojure.org/)** and **[ClojureScript](https://github.com/clojure/clojurescript)** system spanning the **browser** and a backend that runs on the **[JVM](https://en.wikipedia.org/wiki/Java_virtual_machine)**. This project lives in the **[iwaswhere-web directory](https://github.com/matthiasn/iWasWhere/tree/master/iwaswhere-web)**, which is also where you can find installation instructions and more information about the system's architecture.

There's also an **iOS app** that keeps track of visits and lets me quickly capture thoughts on the go. Currently, it's a very basic application written in **[Swift](https://swift.org/)**. It is not open sourced yet, but that will happen in due course. Ideally, by then it should also have been rewritten in **ClojureScript** and **[React Native](https://facebook.github.io/react-native/)**. Stay tuned.



## License
Copyright © 2016 **[Matthias Nehlsen](http://www.matthiasnehlsen.com)**. Distributed under the **GNU GENERAL PUBLIC LICENSE**, Version 3. See separate LICENSE files in sub-projects.
`dommy.template` is a fork of Prismatic's [dommy](https://github.com/Prismatic/dommy) which provides only DOM templating functionality.

All templating features of dommy contained in version 0.1.3 were dropped from version 1.0.0. This fork is also based on 0.1.3 and is kind of complement of dommy 1.0.0: all other features except templating has been dropped. The main goal of this fork is to act as a drop-in replacement for dommy, provideing templating macros `node` and `deftemplate`.

## Usage

Add the following dependency to your `project.clj`:

```clojure
[immoh/dommy.template "0.2.0-SNAPSHOT"]
```

### Migration from `dommy`

Macros `node` and `deftemplate` have been moved from namespace `dommy.macros` to `dommy.template`. Update your imports accordingly.

### Templating

Templating syntax is based on [Hiccup](https://github.com/weavejester/hiccup/), a great HTML library for Clojure. Instead of returning a string of html, dommy's `node` macro returns a DOM node.

```clojure
(ns …
  (:require [dommy.template :as t]))

(t/node
  [:div#id.class1
    (for [r (range 2)]
      [:span.text (str "word" r)])]) ;; => [object HTMLElement]

;; Styles can be inlined as a map
(t/node
  [:span
    {:style
      {:color "#aaa"
       :text-decoration "line-through"}}])
```

The `deftemplate` macro is useful syntactic sugar for defining a function that returns a DOM node.

```clojure
(ns …
  (:require [dommy.template :as t]))

(defn simple-template [cat]
  (t/node [:img {:src cat}]))

(t/deftemplate simple-template [cat]
  [:img {:src cat}])
```

### Type-Hinting Template Macros

One caveat of using the compile-macro is that if you have a compound element (a vector element) and want to have a non-literal map as the attributes (the second element of the vector), then you need to use <code>^:attrs</code> meta-data so the compiler knows to process this symbol as a map of attributes in the runtime system. Here's an example:

```clojure
(t/node [:a ^:attrs (merge m1 m2)])
```

## License

Copyright (C) 2013-2014 Prismatic, 2015 Immo Heikkinen

Distributed under the Eclipse Public License, the same as Clojure.

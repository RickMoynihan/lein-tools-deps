# lein-tools-deps

A leiningen plugin that lets you
share [tools.deps.alpha](https://github.com/clojure/tools.deps.alpha)
dependencies in your leiningen project.

## Usage

Simply add the following to your plugins vector in your `project.clj`:

```clojure
  :plugins [[lein-tools-deps "0.4.0-SNAPSHOT"]]
```

Then set `:tools/deps` to specify which `deps.edn` files to resolve, we recommend:

```
:tools/deps {:config-files [:install :user :project]}
```

The keywords `:install`, `:user` and `:project` will be resolved by the
plugin.  You can also supply your own paths as strings, e.g.

`:tools/deps {:config-files [:install :user :project "../src/deps.edn"]}`

You can now delete your `:dependencies` vector from `project.clj`.

> Note: With `0.3.0-SNAPSHOT` and earlier, the config looked like `:tools/deps [:install :user :project]`

## Prerequisites

You will need the following base dependencies installed:

- Java 8 (recommended)
- Leiningen 2.8.1
- [Clojure 1.9.0+ CLI Tools](https://clojure.org/guides/getting_started)

## Cursive IDE workarounds for macOS

If you're using `lein-tools-deps` with Cursive on macOS you may run into some issues.  Thankfully @mfikes has provided [some workarounds](https://gist.github.com/mfikes/f803fef3013927c376063a3d72b69d60).

## Project Status

**VERY ALPHA**

[![Build Status](https://travis-ci.org/RickMoynihan/lein-tools-deps.svg?branch=master)](https://travis-ci.org/RickMoynihan/lein-tools-deps)

This is almost entirely untested, so don't rely on it yet.  PRs &
ideas for future development welcome.

Please see the [issue tracker](https://github.com/RickMoynihan/lein-tools-deps/issues)

## License

Copyright © 2017 Rick Moynihan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

# lein-tools-deps

A leiningen plugin that lets you
share [tools.deps.alpha](https://github.com/clojure/tools.deps.alpha)
dependencies in your leiningen project.

## Usage

Simply add the following to your plugins vector in your `project.clj`:

```clojure
  :plugins [[lein-tools-deps "0.1.0-SNAPSHOT"]]
```

Then set `:tools/deps` to specify which `deps.edn` files to resolve, we recommend:

`:tools/deps [:system :home :project]`

You can now delete your `:dependencies` vector from `project.clj`.

## Project Status

**VERY ALPHA**

This is almost entirely untested, so don't rely on it yet.  PRs &
ideas for future development welcome.

Please see the [issue tracker](https://github.com/RickMoynihan/lein-tools-deps/issues)

## License

Copyright © 2017 Rick Moynihan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

# lein-tools-deps

[![Clojars Project](https://img.shields.io/clojars/v/lein-tools-deps.svg)](https://clojars.org/lein-tools-deps)

[![Build Status](https://travis-ci.org/RickMoynihan/lein-tools-deps.svg?branch=master)](https://travis-ci.org/RickMoynihan/lein-tools-deps)

A leiningen plugin that lets you
share [tools.deps.alpha](https://github.com/clojure/tools.deps.alpha)
`deps.edn` dependencies with your leiningen project build.

## Why use leiningen and deps.edn?

The Clojure 1.9.0 command line tools bring a host of new benefits to
the Clojure practitioner.  In particular native support for
dependencies hosted in git repositories (and not deployed as maven
artifacts), faster startup times for project REPLs, and easier ways to
script with Clojure and define multi-project projects; all whilst
providing a purely declarative data format in the form of `deps.edn`
files.

However at its core `deps.edn` and the CLI tools are just a simple
system that provide better facilities for resolving dependencies and
building a java classpath.  They actively avoid being a build tool,
and consequently can't be used in isolation to build a project, `:aot`
compile it, `uberjar` it, etc...

Leiningen is the incumbent build tool for Clojure projects.  It's well
supported, with a thriving plugin ecosystem, and is the default choice
in the Clojure world if you need to build an application or deploy a
library.  It's easy to get started with and is great if you have a
pro-forma project; which doesn't need much custom build-scripting.

`lein-tools-deps` teaches Leiningen to take its `:dependencies` from
your `deps.edn` files, which means you can get the best of both
worlds.  You can use `clj` and `deps.edn` to take advantage of
`deps.edn` sub-projects, located on the local filesystem
(`:local/root`) and in git repositories (`:git/url`) or make use of
standard maven dependencies (`:mvn/version`).

`lein-tools-deps` will let you replace your leiningen `:dependencies`
entirely with those from `deps.edn` meaning you don't need to repeat
yourself.  Likewise for `deps.edn` projects if you need to start
`:aot` compiling, `uberjar`ing, or integrating with a `:cljs-build`,
you now can.

Essentially `lein-tools-deps` lets Clojure practitioners use both
`Leiningen` and the `clj` / `deps.edn` tools together in the same
project.

## Why not use boot instead?

Boot is arguably a better choice than Leiningen if you need more bespoke build
scripting.  However Leiningen projects because of their declarative
constraints tend to be more uniform and familiar.  Leiningen projects
are harder to turn into unique snowflakes, which might be better or
worse for you.

If you don't need anything fancy (like a combined Clojurescript/Clojure 
build) and want to just get started quickly, I'd recommend Leiningen 
over Boot.  If you don't need to `:aot`, or to build your Clojure at 
all, and your development environment and prefered tools support it go 
lightweight and just use `clj` and `deps.edn`.

If you want to integrate boot with `tools.deps` you can via @seancorfield's 
[boot-tools-deps](https://github.com/seancorfield/boot-tools-deps/).

## Why not just use deps.edn?

If you can do this, consider it, and consider not using `lein-tools-deps` at
all.  Yes, I'm saying maybe you don't need this project; even if it is pretty
good :-)

`deps.edn` is starting to grow an ecosystem of tools and whilst they're
not yet mature, and the landscape is frequently changing, with various 
contenders many existing tools such as the Clojurescript compiler, and 
figwheel-main have native support for `deps.edn`.  Additionally there are
new tools such as [depstar](https://github.com/healthfinch/depstar), 
[pack.alpha](https://github.com/juxt/pack.alpha), [katamari](https://github.com/arrdem/katamari)
that will work with `deps.edn` and might be able to build that uberjar for you.  

See the [tools.deps Tools](https://github.com/clojure/tools.deps.alpha/wiki/Tools) page
for a more complete list of available tooling.

`lein-tools-deps` is for those who need or want to keep a foot in both 
camps.  Perhaps it's suitable as a stop gap solution for an existing 
leiningen project, or perhaps members of your team are only just 
getting used to leiningen, and you don't want to confuse them with
another tool or workflow.

## More [Frequently Asked Questions...](https://github.com/RickMoynihan/lein-tools-deps/wiki/FAQ)

## Usage

Simply add the following to your plugins and middleware vectors,
respectively, in your `project.clj`:

```clojure
  :plugins [[lein-tools-deps "0.4.3"]]
```

```clojure
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
```

Then set `:lein-tools-deps/config` to specify which `deps.edn` files to resolve, we recommend:

```
:lein-tools-deps/config {:config-files [:install :user :project]}
```

The keywords `:install`, `:user` and `:project` will be resolved by the
plugin.  You can also supply your own paths as strings, e.g.

`:lein-tools-deps/config {:config-files [:install :user :project "../src/deps.edn"]}`

You can now delete your `:dependencies` vector from `project.clj`.

> Note: With `lein-tools-deps` `0.3.0-SNAPSHOT` and earlier, the
> config value was a vector and looked like `:tools/deps [:install
> :user :project]`, as of 0.4.0 it changed to the above map
> based syntax.

### Supported configuration options

#### `:config-files`

A vector referencing an ordered collection of `deps.edn` files that
will be used for dependency resolution.  Each file should be either a
file path string or a special keyword (`:install` `:user` or
`:project`). The special `:install` and `:user` keys refer to the `deps.edn`
files defined in the installation and user's home `.clojure` config directories; 
whilst `:project` refers to a `deps.edn` at the root of your leiningen project.

#### `:clojure-executables`

A vector of strings identifying possible install locations for the
`clojure` command line tool script.  They will be tried in order, with
the first match being used.  The default is currently set to
`[/usr/local/bin/clojure]`.  This is necessary as `lein-tools-deps`
uses the `clojure` executable to determine some system specific
defaults, such as the location of the `:install` `:config-files`.

#### `:resolve-aliases`

A vector of `deps.edn` alias names whose `:extra-deps`, `override-deps`
and `:default-deps` will be resolved with the same semantics as if they
had been used with the `-R` option to the `clj` tool.

#### `:classpath-aliases`

A vector of `deps.edn` alias names whose `:extra-paths` and
`classpath-overrides` will be applied with the same semantics as if
they had been used with the `-C` option to the `clj` tool.

#### `:aliases`

A vector of `deps.edn` alias names whose values are resolved in the same
way as for both `:resolve-aliases` and `classpath-aliases` above.
Equivalent to the `-A` option of the `clj` tool.

### Profiles

`lein-tools-deps` works with Leiningen profiles, allowing you to specify 
dependencies on a per profile basis.  We support the use of any configuration
options in Leiningen profiles, which will follow Leiningen's standard 
`meta-merge` semantics for each of the configuration options above.  Profiles 
are merged before `tools.deps` resolution.

E.g.

```clojure
    :lein-tools-deps/config {:config-files ["foo.edn"]}
    :profiles {:dev {:lein-tools-deps/config ["bar.edn" "baz.edn"]}}
```
results a logical ```:config-files``` value of ```["foo.edn" "bar.edn"
"baz.edn"]```  when the ```:dev``` profile is used.

Aliases and all other options are resolved in a similar fashion, and support
the use of Leiningen's `^:replace`/`^:displace` metadata flags, to control the
merge.

One of the benefits of `lein-tools-deps` is that you can use profiles to group
various combinations of `:aliases` etc under a single profile name.

## Prerequisites

You will need the following base dependencies installed:

- Java 8 (recommended)
- Leiningen 2.8.1
- [Clojure CLI Tools (1.9.0.341 or above)](https://clojure.org/guides/getting_started)

## Project Status

**ALPHA** because `tools.deps` is still `.alpha`.

PRs & ideas for future development welcome.

Please see the [issue tracker](https://github.com/RickMoynihan/lein-tools-deps/issues)

## With thanks to

- @HughPowell
- @mfikes
- @seancorfield
- @puredanger
- @atroche
- @marco-m

## License

Copyright © 2017 Rick Moynihan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

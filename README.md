# core.typed

<a href='http://typedclojure.org'><img src='doc/images/part-of-typed-clojure-project.png'></a>

<p>
  <a href='https://www.patreon.com/ambrosebs'><img src='doc/images/become_a_patron_button.png'></a>
  <a href='https://opencollective.com/typedclojure'><img src='doc/images/donate-to-our-collective.png'></a>
</p>

Optional typing in Clojure, as a library.

## Stable + Maintenance mode

core.typed should be considered stable and has transitioned into maintenance mode.
Further feature development has been moved to the [typedclojure](https://github.com/typedclojure/typedclojure) repository
which was forked from this repository at core.typed `1.0.1`.

See [here](https://github.com/typedclojure/typedclojure/blob/master/UPGRADING.md#from-coretyped-100-to-typedclojure-101) for upgrading instructions.

## Usage Overview

core.typed is separated into modules. You'll want the full type checker at development
time, and the runtime dependencies in production.

In Clojure CLI's `deps.edn`, this will look something like this:

```clojure
{:deps {org.clojure.typed/runtime.jvm {:mvn/version "1.0.1"}}
 :aliases {:dev {:extra-deps {org.clojure.typed/checker.jvm {:mvn/version "1.0.1"}}}}}
```

You can then start a dev repl with `clj -A:dev`.

In Leiningen's `project.clj`, something like this:

```clojure
(defproject a-project "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure.typed/runtime.jvm "1.0.1"]]
  :profiles {:dev {:dependencies [[org.clojure.typed/checker.jvm "1.0.1"]]}})
```

Then, `lein repl` will automatically activate the `:dev` profile. Verify the type
checker is not included in uberjars with `lein with-profile uberjar deps :tree`.

## Releases and Dependency Information

Latest stable release is 1.0.1.

See modules for specific version coordinates:

### Core type system

* [checker.jvm](typed/checker.jvm/README.md): The JVM type checker
* [runtime.jvm](typed/runtime.jvm/README.md): JVM Runtime dependencies
* [analyzer.jvm](typed/analyzer.jvm/README.md): Analyzer for JVM Clojure
* [analyzer.js](typed/analyzer.js/README.md): Analyzer for JS Clojure (unreleased)
* [checker.js](typed/checker.js/README.md): The JS type checker (unreleased)
* [analyzer.common](typed/analyzer.common/README.md): Implementation-agnostic base for Clojure analyzers

### Library Annotations

* [lib.clojure](typed/lib.clojure/README.md): Base type annotations
* [lib.core.async](typed/lib.core.async/README.md): Annotations for core.async

### Utility libraries

* [annotator.jvm](typed/annotator.jvm/README.md): Automatic annotator
* [lang.jvm](typed/lang.jvm/README.md): Extensible languages

## Compatibility

`core.typed` supports Clojure 1.10.1 and JDK 1.8+.

## Mailing List and IRC

Use the core.typed [mailing list](https://groups.google.com/forum/?fromgroups#!forum/clojure-core-typed) for core.typed discussion.

## Documentation

See [wiki](https://github.com/clojure/core.typed/wiki).

[API Reference](http://clojure.github.io/core.typed/)

[Ambrose's blog](http://frenchy64.github.io/)

## Leiningen Plugin

[lein-typed](https://github.com/frenchy64/lein-typed)

## Vim Plugin

[vim-typedclojure](https://github.com/typedclojure/vim-typedclojure)

## Emacs Mode

[typed-clojure-mode](https://github.com/typedclojure/typed-clojure-mode)

## Examples

[core.async Rock-paper-scissors](https://github.com/clojure/core.typed/blob/master/module-check/src/test/clojure/clojure/core/typed/test/rps_async.clj)

## Developer Information

- [Typed Clojure Contributor Code of Conduct](CODE_OF_CONDUCT.md)
- [Contributing guidelines](CONTRIBUTING.md)
- [GitHub Project](https://github.com/clojure/core.typed)
- [Bug Tracker](http://dev.clojure.org/jira/browse/CTYP)
- [Continuous Integration](http://build.clojure.org/job/core.typed/)
- [Compatibility Test Matrix](http://build.clojure.org/job/core.typed-test-matrix/)

<!---
## Future work

* Equality filters for occurrence typing
* Unify AST with ClojureScript
* Namespace dependency management
* Track changes to Typed Racket
  * https://github.com/plt/racket/compare/6105ce8b2087...71d6189132ce
-->

## Contributors

Thanks to the following people for contributing to core.typed:

* Stephen Compall (S11001001)
* Andrew Brehaut (brehaut)
* Christopher Spencer (cspencer)
* Reid McKenzie (arrdem)
* Di Xu (xudifsd)
* Nicola Mometto (Bronsa)
* Chas Emerick (cemerick)
* Jake Goulding (shepmaster)
* Andy Fingerhut
* Aravind K N (arav93)
* Allen Rohner (arohner)
* Minori Yamashita (ympbyc)
* Kyle Kingsbury (aphyr)
* Nathan Sorenson
* Tobias Kortkamp (t6)
* Alejandro Gomez (dialelo)
* Piotr Jarzemski (kazuhiro)

## Sponsors

Thanks to the following companies for their active sponsorship of Typed Clojure's development
via [OpenCollective](https://opencollective.com/typedclojure).

<div>
  <div>
    <a href="https://www.adgoji.com/">
      <img src="https://typedclojure.org/images/sponsors/adgoji.png" alt="Adgoji">
    </a>
  </div>
</div>

## YourKit

YourKit is kindly supporting core.typed and other open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:

* <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
* <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

## License

Copyright © Ambrose Bonnaire-Sergeant, Rich Hickey & contributors.

Licensed under the EPL (see the file epl-v10.html).

### tools.analyzer

Copyright © Nicola Mometto, Rich Hickey & contributors.

Licensed under the EPL (see the file epl-v10.html).

### tools.analyzer.jvm

Copyright © Nicola Mometto, Rich Hickey & contributors.

Licensed under the EPL (see the file epl-v10.html).

### tools.analyzer.js

Copyright © Nicola Mometto, Rich Hickey & contributors.

Licensed under the EPL (see the file epl-v10.html).

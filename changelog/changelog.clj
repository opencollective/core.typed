[[{:version "0.8.0-SNAPSHOT"}
  [:checker.jvm
   "move `{clojure,cljs}.core.typed.async` to `clojure.core.typed.lib.{clojure,cljs}.core.async` in `core.typed.lib.core.async` submodule"
   "remove dependency on tools.analyzer"]
  [:lib.core.async
   "initial release - extracted core.async annotations from `checker.jvm`"]
  [:analyzer.common
   ["Renamed clojure.core.typed.analyzer => clojure.core.typed.analyzer.common"
    ["Note: also applies to namespaced keywords"]]]
  [:analyzer.js
   "extract from checker.js"]
  [:checker.js
   "merge back into monorepo from https://github.com/clojure/core.typed.checker.js"]
  [:all
   ]]]

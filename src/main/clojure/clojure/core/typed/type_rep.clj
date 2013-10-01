(ns ^:skip-wiki 
  clojure.core.typed.type-rep
  (:refer-clojure :exclude [defrecord defprotocol])
  (:require [clojure.core.typed.impl-protocols :as p]
            [clojure.core.typed.utils :as u]
            [clojure.core.typed :as t]
            [clojure.set :as set])
  (:import (clojure.lang Seqable Symbol Keyword Var)))

(t/tc-ignore
(alter-meta! *ns* assoc :skip-wiki true)
  )

(t/ann ^:no-check -FS-var [-> (Var [p/IFilter p/IFilter -> p/IFilterSet])])
(defn- -FS-var []
  (let [ns (find-ns 'clojure.core.typed.filter-ops)
        _ (assert ns)
        v (ns-resolve ns '-FS)]
    (assert (var? v) "-FS unbound")
    v))

(t/ann ^:no-check -top-var [-> (Var p/IFilter)])
(defn -top-var []
  (let [ns (find-ns 'clojure.core.typed.filter-rep)
        _ (assert ns)
        v (ns-resolve ns '-top)]
    (assert (var? v) "-top unbound")
    v))

(t/ann ^:no-check -empty-var [-> (Var p/IRObject)])
(defn -empty-var []
  (let [ns (find-ns 'clojure.core.typed.object-rep)
        _ (assert ns)
        v (ns-resolve ns '-empty)]
    (assert (var? v) "-empty unbound")
    v))

(t/def-alias SeqNumber Long)

;(set! *warn-on-reflection* true)

;;; Type rep predicates

(t/def-alias Type
  "A normal type"
  p/TCType)

(t/def-alias AnyType
  "A normal type or special type like Function."
  (U Type p/TCAnyType))

(t/def-alias MaybeScopedType
  "A type or a scope"
  (U Type p/IScope))

(t/ann-protocol TypeId
                type-id [TypeId -> Long])

(u/defprotocol TypeId
  (type-id [_]))

(t/ann ^:no-check Type? (predicate Type))
(defn Type? [a]
  (instance? clojure.core.typed.impl_protocols.TCType a))

(t/ann ^:no-check AnyType? (predicate AnyType))
(defn AnyType? [a]
  (or (Type? a)
      (instance? clojure.core.typed.impl_protocols.TCAnyType a)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types

(u/ann-record Top [])
(u/def-type Top []
  "The top type"
  []
  :methods
  [p/TCType])

(t/ann -any Type)
(def -any (Top-maker))


(u/ann-record Union [types :- (t/Set Type)])
(u/def-type Union [types]
  "An flattened, unordered union of types"
  [(set? types)
   (every? Type? types)
   (not (some Union? types))]
  :methods
  [p/TCType])

;temporary union maker
(t/ann Un [Type * -> Union])
(defn- Un [& types]
  (Union-maker (set types)))

(t/ann empty-union Type)
(def empty-union (Un))

(t/ann Bottom [-> Type])
(defn Bottom []
  empty-union)

(t/ann -nothing Type)
(def -nothing (Bottom))

(t/ann Bottom? [Any -> Boolean])
(defn Bottom? [a]
  (= empty-union a))

(u/ann-record TCError [])
(u/def-type TCError []
  "Use *only* when a type error occurs"
  []
  :methods
  [p/TCType])

(t/ann Err Type)
(def Err (TCError-maker))


;should probably be ordered
(u/ann-record Intersection [types :- (t/NonEmptySeqable Type)])
(u/def-type Intersection [types]
  "An unordered intersection of types."
  [(seq types)
   (every? Type? types)]
  :methods 
  [p/TCType])

(t/def-alias Variance
  "Keywords that represent a certain variance"
  (U ':constant ':covariant ':contravariant ':invariant ':dotted))

(t/ann variances (t/Set Variance))
(def variances #{:constant :covariant :contravariant :invariant :dotted})

(t/ann ^:no-check variance? (predicate Variance))
(defn variance? [v]
  (contains? variances v))

(declare Scope? TypeFn?)

(u/ann-record Bounds [upper-bound :- MaybeScopedType
                      lower-bound :- MaybeScopedType
                      higher-kind :- nil])

; This annotation needs to go before the first reference to TypeFn,
; otherwise it will resolve to an RClass, instead of a DataType.
; DataType should be combined with RClass in the future.
(u/ann-record TypeFn [nbound :- Number,
                      variances :- (U nil (Seqable Variance))
                      bbnds :- (U nil (Seqable Bounds)),
                      scope :- p/IScope])
(u/def-type Bounds [upper-bound lower-bound higher-kind]
  "A type bound or higher-kind bound on a variable"
  [(every? (some-fn Type? Scope?) [upper-bound lower-bound])
   ;deprecated/unused
   (nil? higher-kind)])

(u/ann-record B [idx :- Number])
(u/def-type B [idx]
  "A bound variable. Should not appear outside this file"
  [(u/nat? idx)]
  :methods
  [p/TCType])

;FIXME rank should be part of the identity of a free, otherwise type caching is unsound
; Same with bounds.
(u/ann-record F [name :- Symbol])
(u/def-type F [name]
  "A named free variable"
  [(symbol? name)]
  :methods
  [p/TCType])

(t/ann make-F [Symbol -> F])
(defn make-F
  "Make a free variable "
  [name] (F-maker name))

(u/ann-record Scope [body :- MaybeScopedType])
(u/def-type Scope [body]
  "A scope that contains one bound variable, can be nested. Not used directly"
  [((some-fn Type? Scope?) body)]
  :methods
  [p/IScope
   (scope-body [this] body)])

(t/ann ^:no-check scope-depth? [Scope Number -> Any])
(defn scope-depth? 
  "True if scope is has depth number of scopes nested"
  [scope depth]
  {:pre [(Scope? scope)
         (u/nat? depth)]}
  (Type? (last (take (inc depth) (iterate #(and (Scope? %)
                                                (:body %))
                                          scope)))))

(u/ann-record RClass [variances :- (U nil (t/NonEmptySeqable Variance))
                      poly? :- (U nil (t/NonEmptySeqable Type))
                      the-class :- Symbol
                      replacements :- (t/Map Symbol MaybeScopedType)
                      unchecked-ancestors :- (t/Set MaybeScopedType)])
(u/def-type RClass [variances poly? the-class replacements unchecked-ancestors]
  "A restricted class, where ancestors are
  (replace replacements (ancestors the-class))"
  [(or (nil? variances)
       (and (seq variances)
            (sequential? variances)
            (every? variance? variances)))
   (or (nil? poly?)
       (and (seq poly?)
            (sequential? poly?)
            (every? Type? poly?)))
   (symbol? the-class)
   ((u/hash-c? symbol? (some-fn Type? Scope?)) replacements)
   ((u/set-c? (some-fn Type? Scope?)) unchecked-ancestors)]
  :intern
  [variances 
   (when poly? (map hash poly?))
   (keyword the-class)
   (into {} (for [[k v] replacements]
              [(keyword k) (hash v)]))
   (into #{} (map hash unchecked-ancestors))]
  :methods
  [p/TCType])

(t/ann ^:no-check RClass->Class [RClass -> Class])
(defn ^Class RClass->Class [^RClass rcls]
  (u/symbol->Class (.the-class rcls)))

(u/ann-record JSNominal [variances :- (U nil (t/NonEmptySeqable Variance))
                         poly? :- (U nil (t/NonEmptySeqable Type))
                         name :- Symbol])
(u/def-type JSNominal [variances poly? name]
  "A Javascript nominal type"
  [(or (nil? variances)
       (and (seq variances)
            (sequential? variances)
            (every? variance? variances)))
   (= (count variances) (count poly?))
   (or (nil? poly?)
       (and (seq poly?)
            (sequential? poly?)
            (every? Type? poly?)))
   (symbol? name)]
  :methods
  [p/TCType])

(u/ann-record DataType [the-class :- Symbol,
                        variances :- (U nil (t/NonEmptySeqable Variance)),
                        poly? :- (U nil (t/NonEmptySeqable Type)),
                        fields :- (t/Map Symbol MaybeScopedType)
                        record? :- Boolean])
(u/def-type DataType [the-class variances poly? fields record?]
  "A Clojure datatype"
  [(or (nil? variances)
       (and (seq variances)
            (every? variance? variances)))
   (or (nil? poly?)
       (and (seq poly?)
            (every? Type? poly?)))
   (= (count variances) (count poly?))
   (symbol? the-class)
   ((u/array-map-c? symbol? (some-fn Scope? Type?)) fields)
   (u/boolean? record?)]
  :methods
  [p/TCType])

(t/ann DataType->Class [DataType -> Class])
(defn ^Class DataType->Class [^DataType dt]
  (u/symbol->Class (.the-class dt)))

(t/ann Record? [Any -> Boolean])
(defn Record? [^DataType a]
  (boolean
    (when (DataType? a)
      (.record? a))))

(u/ann-record Protocol [the-var :- Symbol,
                        variances :- (U nil (t/NonEmptySeqable Variance)),
                        poly? :- (U nil (t/NonEmptySeqable Type)),
                        on-class :- Symbol,
                        methods :- (t/Map Symbol Type)
                        declared? :- Boolean])
(u/def-type Protocol [the-var variances poly? on-class methods declared?]
  "A Clojure Protocol"
  [(symbol? the-var)
   (or (nil? variances)
       (and (seq variances)
            (every? variance? variances)))
   (or (nil? poly?)
       (and (seq poly?)
            (every? Type? poly?)))
   (= (count poly?) (count variances))
   (symbol? on-class)
   ((u/hash-c? (every-pred symbol? (complement namespace)) Type?) methods)
   (u/boolean? declared?)]
  :methods
  [p/TCType])

(u/def-type TypeFn [nbound variances bbnds scope]
  "A type function containing n bound variables with variances.
  It is of a higher kind"
  [(u/nat? nbound)
   (every? variance? variances)
   (every? Bounds? bbnds)
   (apply = nbound (map count [variances bbnds]))
   (scope-depth? scope nbound)
   (Scope? scope)]
  :methods
  [p/TCType])

;FIXME actual-frees should be metadata. ie. it should not affect equality
(u/ann-record Poly [nbound :- Number,
                    bbnds :- (U nil (Seqable Bounds)),
                    scope :- p/IScope,
                    actual-frees :- (U nil (Seqable Symbol))])
(u/def-type Poly [nbound bbnds scope actual-frees]
  "A polymorphic type containing n bound variables, with display names actual-frees"
  [(u/nat? nbound)
   (every? Bounds? bbnds)
   (every? symbol? actual-frees)
   (apply = nbound (map count [bbnds actual-frees]))
   (scope-depth? scope nbound)
   (Scope? scope)]
  :methods
  [p/TCType])

(u/ann-record PolyDots [nbound :- Number,
                        bbnds :- (U nil (Seqable Bounds)),
                        scope :- p/IScope
                        actual-frees :- (U nil (Seqable Symbol))])
(u/def-type PolyDots [nbound bbnds ^Scope scope actual-frees]
  "A polymorphic type containing n-1 bound variables and 1 ... variable"
  [(u/nat? nbound)
   (every? Bounds? bbnds)
   (every? symbol? actual-frees)
   (= nbound (count bbnds))
   (scope-depth? scope nbound)
   (Scope? scope)]
  :methods
  [p/TCType])

(u/ann-record Name [id :- Symbol])
(u/def-type Name [id]
  "A late bound name"
  [((every-pred (some-fn namespace (fn [a] (some (fn [c] (= \. c)) (str a))))
                symbol?) 
     id)]
  :methods
  [p/TCType])

(u/ann-record TApp [rator :- Type,
                    rands :- (Seqable Type)])
(u/def-type TApp [rator rands]
  "An application of a type function to arguments."
  [((some-fn Name? TypeFn? F? B? Poly?) rator)
   (every? (some-fn TypeFn? Type?) rands)]
  :methods
  [p/TCType]);not always a type

(u/ann-record App [rator :- Type,
                   rands :- (Seqable Type)])
(u/def-type App [rator rands]
  "An application of a polymorphic type to type arguments"
  [(Type? rator)
   (every? Type? rands)]
  :methods
  [p/TCType])

(u/ann-record Mu [scope :- p/IScope])
(u/def-type Mu [scope]
  "A recursive type containing one bound variable, itself"
  [(Scope? scope)]
  :methods
  [p/TCType
   p/IMu
   (mu-scope [_] scope)])

(u/ann-record Value [val :- Any])
(u/def-type Value [val]
  "A Clojure value"
  []
  :methods
  [p/TCType])

(u/ann-record AnyValue [])
(u/def-type AnyValue []
  "Any Value"
  []
  :methods
  [p/TCType])

(t/ann -val [Any -> Type])
(def -val Value-maker)

(t/ann-many Type 
            -false -true -nil)
(def -false (-val false))
(def -true (-val true))
(def -nil (-val nil))

(t/ann-many [Any -> Boolean]
            Nil? False? True?)
(defn Nil? [a] (= -nil a))
(defn False? [a] (= -false a))
(defn True? [a] (= -true a))

(declare Result?)

(u/ann-record HeterogeneousMap [types :- (t/Map Type Type),
                                absent-keys :- (t/Set Type),
                                other-keys? :- Boolean])
(u/def-type HeterogeneousMap [types absent-keys other-keys?]
  "A constant map, clojure.lang.IPersistentMap"
  [((u/hash-c? Value? (some-fn Type? Result?))
     types)
   ((u/set-c? Value?) absent-keys)
   (u/boolean? other-keys?)]
  :methods
  [p/TCType])

(u/ann-record HeterogeneousVector [types :- (t/Vec Type)
                                   fs :- (t/Vec p/IFilterSet)
                                   objects :- (t/Vec p/IRObject)])
(u/def-type HeterogeneousVector [types fs objects]
  "A constant vector, clojure.lang.IPersistentVector"
  [(vector? types)
   (every? (some-fn Type? Result?) types)
   (vector? fs)
   (every? p/IFilterSet? fs)
   (vector? objects)
   (every? p/IRObject? objects)
   (apply = (map count [types fs objects]))]
  :methods
  [p/TCType])

(t/ann ^:no-check -hvec 
       [(t/Vec Type) & :optional {:filters (Seqable p/IFilterSet) :objects (Seqable p/IRObject)} -> Type])
(defn -hvec 
  [types & {:keys [filters objects]}]
  (let [-FS @(-FS-var)
        -top @(-top-var)
        -empty @(-empty-var)]
    (if (some Bottom? types)
      (Bottom)
      (HeterogeneousVector-maker types
                             (if filters
                               (vec filters)
                               (vec (repeat (count types) (-FS -top -top))))
                             (if objects
                               (vec objects)
                               (vec (repeat (count types) -empty)))))))

(u/ann-record HeterogeneousList [types :- (Seqable Type)])
(u/def-type HeterogeneousList [types]
  "A constant list, clojure.lang.IPersistentList"
  [(sequential? types)
   (every? Type? types)]
  :methods
  [p/TCType])

(u/ann-record HeterogeneousSeq [types :- (Seqable Type)])
(u/def-type HeterogeneousSeq [types]
  "A constant seq, clojure.lang.ISeq"
  [(sequential? types)
   (every? Type? types)]
  :methods
  [p/TCType])

(u/ann-record PrimitiveArray [jtype :- Class,
                              input-type :- Type
                              output-type :- Type])
(u/def-type PrimitiveArray [jtype input-type output-type]
  "A Java Primitive array"
  [(class? jtype)
   (Type? input-type)
   (Type? output-type)]
  :methods
  [p/TCType])

(u/ann-record DottedPretype [pre-type :- Type,
                             name :- (U Symbol Number)])
(u/def-type DottedPretype [pre-type name]
  "A dotted pre-type. Not a type"
  [(Type? pre-type)
   ((some-fn symbol? u/nat?) name)]
  :methods
  [p/TCAnyType])

;not a type, see KwArgsSeq
(u/ann-record KwArgs [mandatory :- (t/Map Type Type)
                      optional  :- (t/Map Type Type)])
(u/def-type KwArgs [mandatory optional]
  "A set of mandatory and optional keywords"
  [(every? (u/hash-c? Value? Type?) [mandatory optional])
   (= #{} (set/intersection (set (keys mandatory)) 
                            (set (keys optional))))])

(u/ann-record KwArgsSeq [mandatory :- (t/Map Type Type)
                         optional  :- (t/Map Type Type)])
(u/def-type KwArgsSeq [mandatory optional]
  "A sequential seq representing a flattened map (for keyword arguments)."
  [(every? (u/hash-c? Value? Type?) [mandatory optional])
   (= #{} (set/intersection (set (keys mandatory)) 
                            (set (keys optional))))]
  :methods
  [p/TCType])

; must go before Result
(u/ann-record FlowSet [normal :- p/IFilter])

;must go before Function
(u/ann-record Result [t :- Type,
                      fl :- p/IFilterSet
                      o :- p/IRObject
                      flow :- FlowSet])

(u/ann-record Function [dom :- (U nil (Seqable Type)),
                        rng :- Result,
                        rest :- (U nil Type)
                        drest :- (U nil DottedPretype)
                        kws :- (U nil KwArgs)])
(u/def-type Function [dom rng rest drest kws]
  "A function arity, must be part of an intersection"
  [(or (nil? dom)
       (sequential? dom))
   (every? Type? dom)
   (Result? rng)
   ;at most one of rest drest or kws can be provided
   (#{0 1} (count (filter identity [rest drest kws])))
   (or (nil? rest)
       (Type? rest))
   (or (nil? drest)
       (DottedPretype? drest))
   (or (nil? kws)
       (KwArgs? kws))]
  :methods
  [p/TCAnyType])

(u/ann-record TopFunction [])
(u/def-type TopFunction []
  "Supertype to all functions"
  [])

(u/ann-record FnIntersection [types :- (t/NonEmptySeqable Function)])
(u/def-type FnIntersection [types]
  "An ordered intersection of Functions."
  [(seq types)
   (sequential? types)
   (every? Function? types)]
  :methods
  [p/TCType])

(u/ann-record CountRange [lower :- Number,
                          upper :- (U nil Number)])
(u/def-type CountRange [lower upper]
  "A sequence of count between lower and upper.
  If upper is nil, between lower and infinity."
  [(u/nat? lower)
   (or (nil? upper)
       (and (u/nat? upper)
            (<= lower upper)))]
  :methods
  [p/TCType])

(u/ann-record GTRange [n :- Number])
(u/def-type GTRange [n]
  "The type of all numbers greater than n"
  [(number? n)]
  :methods
  [p/TCType])

(u/ann-record LTRange [n :- Number])
(u/def-type LTRange [n]
  "The type of all numbers less than n"
  [(number? n)]
  :methods
  [p/TCType])

(t/ann make-CountRange (Fn [Number -> CountRange]
                           [Number (U nil Number) -> CountRange]))
(defn make-CountRange
  ([lower] (make-CountRange lower nil))
  ([lower upper] (CountRange-maker lower upper)))

(t/ann make-ExactCountRange (Fn [Number -> CountRange]))
(defn make-ExactCountRange [c]
  {:pre [(u/nat? c)]}
  (make-CountRange c c))

(declare Result-maker)

(t/ann ^:no-check make-FnIntersection [Function * -> FnIntersection])
(defn make-FnIntersection [& fns]
  {:pre [(every? Function? fns)]}
  (FnIntersection-maker fns))

(u/ann-record NotType [type :- Type])
(u/def-type NotType [type]
  "A type that does not include type"
  [(Type? type)]
  :methods
  [p/TCType])

(u/ann-record ListDots [pre-type :- Type,
                        bound :- (U F B)])
(u/def-type ListDots [pre-type bound]
  "A dotted list"
  [(Type? pre-type)
   ((some-fn F? B?) bound)]
  :methods
  [p/TCType])

(u/ann-record Extends [extends :- (I (CountRange 1) (Seqable Type))
                       without :- (U nil (Seqable Type))])
(u/def-type Extends [extends without]
  "A set of ancestors that always and never occur."
  [(every? Type? extends)
   (seq extends)
   (every? Type? without)]
  :methods
  [p/TCType])

(declare FlowSet?)

(u/def-type Result [t fl o flow]
  "A result type with filter f and object o. NOT a type."
  [(Type? t)
   (p/IFilterSet? fl)
   (p/IRObject? o)
   (FlowSet? flow)]
  :methods
  [p/TCAnyType])

(declare ret TCResult?)

(u/ann-record TCResult [t :- Type
                        fl :- p/IFilterSet?
                        o :- p/IRObject
                        flow :- FlowSet])

(t/ann Result->TCResult [Result -> TCResult])
(defn Result->TCResult [{:keys [t fl o] :as r}]
  {:pre [(Result? r)]
   :post [(TCResult? %)]}
  (ret t fl o))

(t/ann Result-type* [Result -> Type])
(defn Result-type* [r]
  {:pre [(Result? r)]
   :post [(Type? %)]}
  (:t r))

(t/ann ^:no-check Result-filter* [Result -> p/IFilter])
(defn Result-filter* [r]
  {:pre [(Result? r)]
   :post [(p/IFilter? %)]}
  (:fl r))

(t/ann ^:no-check Result-object* [Result -> p/IRObject])
(defn Result-object* [r]
  {:pre [(Result? r)]
   :post [(p/IRObject? %)]}
  (:o r))

(t/ann ^:no-check Result-flow* [Result -> FlowSet])
(defn Result-flow* [r]
  {:pre [(Result? r)]
   :post [(FlowSet? %)]}
  (:flow r))

(t/ann no-bounds Bounds)
(def no-bounds (Bounds-maker -any (Un) nil))

(t/ann -bounds [Type Type -> Bounds])
(defn -bounds [u l]
  (Bounds-maker u l nil))

;unused
(t/tc-ignore
(defonce ^:dynamic *mutated-bindings* #{})

(defn is-var-mutated? [id]
  (contains? *mutated-bindings* id))
  )

(u/def-type FlowSet [normal]
  "The filter that is true when an expression returns normally ie. not an exception."
  [(p/IFilter? normal)]
  :methods
  [p/IFilter])

(u/def-type TCResult [t fl o flow]
  "This record represents the result of typechecking an expression"
  [(Type? t)
   (p/IFilterSet? fl)
   (p/IRObject? o)
   (FlowSet? flow)]
  :methods
  [p/TCAnyType])

(t/ann -flow [p/IFilter -> FlowSet])
(defn -flow [normal]
  (FlowSet-maker normal))

(t/ann ^:no-check ret
       (Fn [Type -> TCResult]
           [Type p/IFilterSet -> TCResult]
           [Type p/IFilterSet p/IRObject -> TCResult]
           [Type p/IFilterSet p/IRObject FlowSet -> TCResult]))
(defn ret
  "Convenience function for returning the type of an expression"
  ([t] (let [-FS @(-FS-var)
             -top @(-top-var)
             -empty @(-empty-var)]
         (ret t (-FS -top -top) -empty (-flow -top))))
  ([t f] 
   (let [-top @(-top-var)
         -empty @(-empty-var)]
     (ret t f -empty (-flow -top))))
  ([t f o] 
   (let [-top @(-top-var)]
     (ret t f o (-flow -top))))
  ([t f o flow]
   {:pre [(AnyType? t)
          (p/IFilterSet? f)
          (p/IRObject? o)
          (FlowSet? flow)]
    :post [(TCResult? %)]}
   (TCResult-maker t f o flow)))

(t/ann ret-t [TCResult -> Type])
(defn ret-t [r]
  {:pre [(TCResult? r)]
   :post [(AnyType? %)]}
  (:t r))

(t/ann ^:no-check ret-f [TCResult -> p/IFilterSet])
(defn ret-f [r]
  {:pre [(TCResult? r)]
   :post [(p/IFilterSet? %)]}
  (:fl r))

(t/ann ^:no-check ret-o [TCResult -> p/IRObject])
(defn ret-o [r]
  {:pre [(TCResult? r)]
   :post [(p/IRObject? %)]}
  (:o r))

(t/ann ret-flow [TCResult -> FlowSet])
(defn ret-flow [r]
  {:pre [(TCResult? r)]
   :post [(FlowSet? %)]}
  (:flow r))

(t/ann ^:no-check flow-normal [FlowSet -> p/IFilter])
(defn flow-normal [f]
  {:pre [(FlowSet? f)]
   :post [(p/IFilter? %)]}
  (:normal f))

;; Utils
;; It seems easier to put these here because of dependencies

(t/ann ^:no-check visit-bounds [Bounds [Type -> Type] -> Bounds])
(defn visit-bounds 
  "Apply f to each element of bounds"
  [ty f]
  {:pre [(Bounds? ty)]
   :post [(Bounds? ty)]}
  (-> ty
    (update-in [:upper-bound] #(when %
                                 (f %)))
    (update-in [:lower-bound] #(when %
                                 (f %)))
    (update-in [:higher-kind] #(when %
                                 (f %)))))

(t/ann ^:no-check make-Result
       (Fn [Type -> Result]
           [Type (U nil p/IFilter) -> Result]
           [Type (U nil p/IFilter) (U nil p/IRObject) -> Result]
           [Type (U nil p/IFilter) (U nil p/IRObject) (U nil FlowSet) -> Result]))
(defn make-Result
  "Make a result. ie. the range of a Function"
  ([t] (make-Result t nil nil nil))
  ([t f] (make-Result t f nil nil))
  ([t f o] (make-Result t f o nil))
  ([t f o flow]
   (let [-FS @(-FS-var)
         -top @(-top-var)
         -empty @(-empty-var)]
     (Result-maker t (or f (-FS -top -top)) (or o -empty) (or flow (-flow -top))))))

(t/ann ^:no-check make-Function
       (Fn [(U nil (Seqable Type)) Type -> Function]
           [(U nil (Seqable Type)) Type (U nil Type) -> Function]
           [(U nil (Seqable Type)) Type (U nil Type) (U nil Type) 
            & :optional 
              {:filter (U nil p/IFilterSet) :object (U nil p/IRObject)
               :flow (U nil FlowSet)
               :mandatory-kws (U nil (t/Map Type Type))
               :optional-kws (U nil (t/Map Type Type))}
            -> Function]))
(defn make-Function
  "Make a function, wrap range type in a Result.
  Accepts optional :filter and :object parameters that default to the most general filter
  and EmptyObject"
  ([dom rng] (make-Function dom rng nil nil))
  ([dom rng rest] (make-Function dom rng rest nil))
  ([dom rng rest drest & {:keys [filter object mandatory-kws optional-kws flow]}]
   (let [-FS @(-FS-var)
         -top @(-top-var)
         -empty @(-empty-var)]
     (Function-maker dom (make-Result rng filter object flow)
                     rest drest (when (or mandatory-kws optional-kws)
                                  (KwArgs-maker (or mandatory-kws {})
                                            (or optional-kws {})))))))


;;;;;;;;;;;;;;;;;
;; Clojurescript types

(u/ann-record BooleanCLJS [])
(u/def-type BooleanCLJS []
  "Primitive boolean in CLJS"
  []
  :methods
  [p/TCType])

(u/ann-record ObjectCLJS [])
(u/def-type ObjectCLJS []
  "Primitive object in CLJS"
  []
  :methods
  [p/TCType])

(u/ann-record StringCLJS [])
(u/def-type StringCLJS []
  "Primitive string in CLJS"
  []
  :methods
  [p/TCType])

(u/ann-record NumberCLJS [])
(u/def-type NumberCLJS []
  "Primitive number in CLJS"
  []
  :methods
  [p/TCType])

(u/ann-record IntegerCLJS [])
(u/def-type IntegerCLJS []
  "Primitive integer in CLJS"
  []
  :methods
  [p/TCType])

(u/ann-record ArrayCLJS [input-type :- Type
                         output-type :- Type])
(u/def-type ArrayCLJS [input-type output-type]
  "Primitive array in CLJS"
  [(Type? input-type)
   (Type? output-type)]
  :methods
  [p/TCType])

(u/def-type FunctionCLJS []
  "Primitive function in CLJS"
  []
  :methods
  [p/TCType])
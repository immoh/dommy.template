(ns dommy.macros
  (:require [clojure.string :as str]))

(declare node)

(defn as-str
  "Coerces strings and keywords to strings, while preserving namespace of
   namespaced keywords"
  [s]
  (if (keyword? s)
    (str (some-> (namespace s) (str "/")) (name s))
    s))

(def +default-ns+ "http://www.w3.org/1999/xhtml")
(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defmacro compile-add-attr!
  "compile-time add attribute"
  [d k v]
  (assert (keyword? k))
  `(when ~v
     ~(cond
       (identical? k :class) `(set! (.-className ~d) (.trim (str (.-className ~d) " " ~v)))
       (identical? k :style) `(.setAttribute ~d ~(as-str k) (dommy.template/style-str ~v))
       ;; If we can compile into a single string at compile time, then make single string
       ;; and set it. Otherwise, need to fall back to calling runtime set-attr! for each class
       (identical? k :classes) (if (every? #(or (string? %) (keyword? %)) v)
                                 `(compile-add-attr! ~d :class ~(str/join " " (map as-str v)))
                                 `(compile-add-attr! ~d :class (str/join " " ~v)))
       :else `(.setAttribute ~d ~(as-str k) ~v))))

(defn parse-keyword
  "return pair [tag class-str id] where tag is dom tag and attrs
   are key-value attribute pairs from css-style dom selector"
  [node-key]
  (let [node-str (as-str node-key)
        node-tag (second (re-find #"^([^.\#]+)[.\#]?" node-str))
        classes (map #(.substring ^String % 1) (re-seq #"\.[^.*]*" node-str))
        id (first (map #(.substring ^String % 1) (re-seq #"#[^.*]*" node-str)))]
    [(if (empty? node-tag) "div" node-tag)
     (str/join " " classes)
     id]))

(defmacro compile-compound [[node-key & rest]]
  (let [literal-attrs (when (map? (first rest)) (first rest))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (drop (if (or literal-attrs var-attrs) 1 0) rest)
        [tag class-str id] (parse-keyword node-key)
        dom-sym (gensym "dom")
        element-ns (if (+svg-tags+ tag) +svg-ns+ +default-ns+)]
    `(let [~dom-sym (.createElementNS js/document ~element-ns ~(as-str tag))]
       ~@(when-not (empty? class-str)
           [`(set! (.-className ~dom-sym) ~class-str)])
       ~@(when id
           [`(.setAttribute ~dom-sym "id" ~id)])
       ~@(for [[k v] literal-attrs]
           (if (keyword? k)
             `(compile-add-attr! ~dom-sym ~k ~v)
             `(dommy.template/set-attr! ~dom-sym ~k ~v)))
       ~@(when var-attrs
           [`(doseq [[k# v#] ~var-attrs]
               (dommy.template/set-attr! ~dom-sym k# v#))])
       ~@(for [c children]
           `(.appendChild ~dom-sym (node ~c)))
       ~dom-sym)))

(defmacro node [data]
  (cond
   (= (str data) "js/document") `js/document
   (vector? data) `(compile-compound ~data)
   (keyword? data) `(compile-compound [~data])
   (or (string? data) (:text (meta data))) `(.createTextNode js/document ~data)
   :else `(dommy.template/->node-like ~data)))

(defmacro deftemplate [name args & node-forms]
  `(defn ~name ~args
     ~(if (next node-forms)
        (let [doc-frag (gensym "frag")]
          `(let [~doc-frag (.createDocumentFragment js/document)]
             ~@(for [el node-forms]
                 `(.appendChild ~doc-frag (node ~el)))
             ~doc-frag))
        `(node ~(first node-forms)))))

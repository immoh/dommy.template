(ns dommy.attrs
  (:use-macros
   [dommy.macros :only [node]])
  (:require
   [clojure.string :as str]
   [dommy.utils :refer [as-str]]))

(defn- ^boolean class-match?
  "does class-name string have class starting at index idx.
   only will be used when Element::classList doesn't exist"
  [class-name class idx]
  (and
   ;; start
   (or (zero? idx) (identical? \space (.charAt class-name (dec idx))))
   ;; stop
   (let [total-len (.-length class-name)
         stop (+ idx (.-length class))]
     (when (<= stop total-len)
       (or (identical? stop total-len)
           (identical? \space (.charAt class-name stop)))))))

(defn- class-index
  "Finds the index of class in a space-delimited class-name
    only will be used when Element::classList doesn't exist"
  [class-name class]
  (loop [start-from 0]
    (let [i (.indexOf class-name class start-from)]
      (when (>= i 0)
        (if (class-match? class-name class i)
          i
          (recur (+ i (.-length class))))))))

(defn add-class!
  "add class to element"
  ([elem classes]
     (let [elem (node elem)
           classes (-> classes as-str str/trim)]
       (when (seq classes)
         (if-let [class-list (.-classList elem)]
           (doseq [class (.split classes #"\s+")]
             (.add class-list class))
           (doseq [class (.split classes #"\s+")]
             (let [class-name (.-className elem)]
               (when-not (class-index class-name class)
                 (set! (.-className elem)
                       (if (identical? class-name "")
                         class
                         (str class-name " " class))))))))
       elem))
  ([elem classes & more-classes]
     (let [elem (node elem)]
       (doseq [c (conj more-classes classes)]
         (add-class! elem c))
       elem)))

(defn style-str [x]
  (if (string? x)
    x
    (->> x
         (map (fn [[k v]] (str (as-str k) ":" (as-str v) ";")))
         (str/join " "))))

(defn set-attr!
  "Sets dom attributes on and returns `elem`.
   Attributes without values will be set to \"true\":

       (set-attr! elem :disabled)

   With values, the function takes variadic kv pairs:

       (set-attr! elem :id \"some-id\"
                       :name \"some-name\")"
  ([elem k] (set-attr! (node elem) k "true"))
  ([elem k v]
     (when v
       (if (fn? v)
         (doto (node elem)
           (aset (as-str k) v))
         (doto (node elem)
           (.setAttribute
            (as-str k)
            (if (= k :style)
              (style-str v)
              v))))))
  ([elem k v & kvs]
     (assert (even? (count kvs)))
     (let [elem (node elem)]
       (doseq [[k v] (->> kvs (partition 2) (cons [k v]))]
         (set-attr! elem k v))
       elem)))

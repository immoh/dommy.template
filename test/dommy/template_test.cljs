(ns dommy.template-test
  (:use-macros
   [cemerick.cljs.test :only [is deftest]]
   [dommy.template :only [deftemplate node]])
  (:require
   [cemerick.cljs.test :as test]
   [dommy.template :as template]))


(deftest simple-template
  ;; unfortunately to satisfy the macro gods, you need to
  ;; duplicate the vector literal to test compiled and runtime template
  (let [e (node [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
  (let [e (node
            [:a {:classes ["class1" "class2"] :href "http://somelink"} "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))
    (is (= "class1 class2" (.-className e))))
  (let [e1 (template/base-element :div#id.class1.class2)
        e2 (node :div#id.class1.class2)]
    (doseq [e [e1 e2]]
      (is (= "DIV" (.-tagName e)))
      (is (= "id" (.getAttribute e "id")))
      (is (= "class1 class2" (.-className e)))))
  (let [e (node [:div#id {:class "class1 class2"}])]
    (is (= "class1 class2" (.-className e))))
  (let [e1 (template/compound-element [:div {:style {:margin-left "15px"}}])
        e2 (node [:div {:style {:margin-left "15px"}}])]
    (doseq [e [e1 e2]]
      (is (= "DIV" (.-tagName e)))
      (is (= "margin-left:15px;" (.getAttribute e "style")))))
  (let [e (template/compound-element [:div (interpose [:br] (repeat 3 "test"))])]
    (is (-> e .-outerHTML (= "<div>test<br>test<br>test</div>"))))
  (let [e1 (template/compound-element [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])
        e2 (node [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (doseq [e [e1 e2]]
      (is (= "span1span2" (.-textContent e)))
      (is (= "class1" (.-className e)))
      (is (= 2 (-> e .-childNodes .-length)))
      (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
             (.-innerHTML e)))
      (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
      (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML)))))
  (let [e (first (template/html->nodes "<div><p>some-text</p></div>"))]
    (is (= "DIV" ( .-tagName e)))
    (is (= "<p>some-text</p>" (.-innerHTML e))))
  (let [comment (first (template/html->nodes "<!--a comment should not throw an exception-->"))]
    (is (= "a comment should not throw an exception" (.-textContent comment)))))

(deftest nested-template-test
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        h   [:div#id1.class1 (list spans end)]
        e (template/compound-element h)]
    (is (-> e .-textContent (= "span0span1end")))
    (is (-> e .-className (= "class1")))
    (is (-> e .-childNodes .-length (= 3)))
    (is (-> e .-innerHTML
            (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>")))
    (is (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
    (is (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
    (is (-> e .-childNodes (aget 2) .-innerHTML (= "end"))))

  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        h1    [:div.class1 (list spans end)]
        h2    [:div.class1 spans end]
        e11 (template/compound-element h1)
        e12 (node             h1)
        e21 (template/compound-element h2)
        e22 (node             h2)]
    (doseq [[e1 e2] [[e11 e12]
                     [e12 e21]
                     [e21 e22]
                     [e22 e11]]]
      (is (= (.-innerHTML e1) (.-innerHTML e2))))))

(deftest boolean-attribute
  (let [e1 (node [:option {:selected true} "some text"])
        e2 (node [:option {:selected false} "some text"])
        e3 (node [:option {:selected nil} "some text"])]
    (is (-> e1 (.getAttribute "selected") (= "true")))
    (is (-> e2 (.getAttribute "selected") (nil?)))
    (is (-> e3 (.getAttribute "selected") (nil?)))))

(deftest style-str
  (let [e1 (node [:div {:style {:background-color "lime"}}])
        e2 (node [:div {:style {:background-color :lime}}])
        e3 (node [:div {:style "background-color: lime"}])
        e4 (node [:div {:style nil}])]
    (is (= (.getAttribute e1 "style") "background-color:lime;"))
    (is (= (.getAttribute e2 "style") "background-color:lime;"))
    (is (= (.getAttribute e3 "style") "background-color: lime"))
    (is (nil? (.getAttribute e4 "style")))))

(deftemplate simple-template [[href anchor]]
  [:a.anchor {:href href} ^:text anchor])

(deftest  deftemplate
  (let [elem (simple-template ["http://somelink.html" "some-text"])]
    (is (= (.-className elem) "anchor"))
    (is (= (.-href elem) "http://somelink.html/"))
    (is (= (.-text elem) "some-text"))))

(deftemplate nested-template [n]
  [:ul.class1 (for [i (range n)] [:li i])])

(deftest nested-deftemplate
  (is (= "<ul class=\"class1\"><li>0</li><li>1</li><li>2</li><li>3</li><li>4</li></ul>"
         (.-outerHTML (nested-template 5)))))


(deftemplate compound-template []
  [:span "foo"]
  [:span "bar"])

(deftest compound-template-test
  (let [frag (compound-template)]
    (is (= 2 (-> frag .-childNodes .-length)))
    (is (= "<span>foo</span>" (-> frag .-firstChild .-outerHTML)))
    (is (= "<span>bar</span>" (-> frag .-lastChild .-outerHTML)))))

(deftemplate single-template-expression []
  (for [s ["foo" "bar"]] [:span s]))

(deftest single-template-expression-test
  (let [frag (single-template-expression)]
    (is (= 2 (-> frag .-childNodes .-length)))
    (is (= "<span>foo</span>" (-> frag .-firstChild .-outerHTML)))
    (is (= "<span>bar</span>" (-> frag .-lastChild .-outerHTML)))))

(deftemplate compound-template-expressions []
  (for [s ["foo" "bar"]] [:span s])
  [:span "wtf"])

(deftest compound-template-expressions-test
  (let [frag (compound-template-expressions)]
    (is (= 3 (-> frag .-childNodes .-length)))
    (is (= "<span>foo</span>" (-> frag .-firstChild .-outerHTML)))
    (is (= "<span>wtf</span>" (-> frag .-lastChild .-outerHTML)))))

(deftemplate nil-template []
  nil)

(deftest nil-in-template
  (is (= "<span></span>"
         (.-outerHTML (node [:span nil]))))
  (is (= "<ul><li>0</li><li>2</li></ul>"
         (.-outerHTML (node [:ul (for [i (range 3)]
                                            (when (even? i)
                                              [:li i]))])))))
(deftest nil-template-test
  (is (= 0 (-> (nil-template) .-childNodes .-length))))

(deftemplate span-wrapper [content]
  [:span content])

(deftest empty-string-in-template
  (is (= "<span></span>"
         (.-outerHTML (span-wrapper "")))))


(deftemplate classes-attr-template [x y]
  [:div
   {:classes [(str "class-" x) (str "class-" y)]}])

(deftemplate classes-compilable-attr-template []
  [:div
   {:classes [:c1 :c2]}])

(deftest classes-attr
  (is (= "class-42 class-43" (.-className (classes-attr-template 42 43))))
  (is (= "c1 c2" (.-className (classes-compilable-attr-template)))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (node [:p]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (node [:circle])))))

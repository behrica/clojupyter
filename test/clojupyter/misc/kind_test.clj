(ns clojupyter.misc.kind-test
  (:require
   [clojupyter.misc.kind :as k]
   [clojure.string :as str]
   [midje.sweet                    :refer [=> facts]]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly-render.note.to-hiccup-js :as to-hiccup-js]
   [scicloj.kindly.v4.kind :as kind]
   [scicloj.tableplot.v1.plotly :as plotly]
   [tablecloth.api :as tc]
   [reagent.core]
   [scicloj.kindly-advice.v1.api :as kindly-advice]
   [hiccup.core :as hiccup]
   [clojure.string :as s]
   [scicloj.kindly-render.shared.walk :as walk]))

(def raw-image
  (->  "https://upload.wikimedia.org/wikipedia/commons/e/eb/Ash_Tree_-_geograph.org.uk_-_590710.jpg"
       (java.net.URL.)
       (javax.imageio.ImageIO/read)))

(def image
  (kind/image raw-image))

(def cs
  (kind/cytoscape
   {:elements {:nodes [{:data {:id "a" :parent "b"} :position {:x 215 :y 85}}
                       {:data {:id "b"}}
                       {:data {:id "c" :parent "b"} :position {:x 300 :y 85}}
                       {:data {:id "d"} :position {:x 215 :y 175}}
                       {:data {:id "e"}}
                       {:data {:id "f" :parent "e"} :position {:x 300 :y 175}}]
               :edges [{:data {:id "ad" :source "a" :target "d"}}
                       {:data {:id "eb" :source "e" :target "b"}}]}
    :style [{:selector "node"
             :css {:content "data(id)"
                   :text-valign "center"
                   :text-halign "center"}}
            {:selector "parent"
             :css {:text-valign "top"
                   :text-halign "center"}}
            {:selector "edge"
             :css {:curve-style "bezier"
                   :target-arrow-shape "triangle"}}]
    :layout {:name "preset"
             :padding 5}}))

(def vega-spec
  {:$schema "https://vega.github.io/schema/vega/v5.json"
   :width 400
   :height 200
   :padding 5
   :data {:name "table"
          :values [{:category :A :amount 28}
                   {:category :B :amount 55}
                   {:category :C :amount 43}
                   {:category :D :amount 91}
                   {:category :E :amount 81}
                   {:category :F :amount 53}
                   {:category :G :amount 19}
                   {:category :H :amount 87}]}
   :signals [{:name :tooltip
              :value {}
              :on [{:events "rect:mouseover"
                    :update :datum}
                   {:events "rect:mouseout"
                    :update "{}"}]}]
   :scales [{:name :xscale
             :type :band
             :domain {:data :table
                      :field :category}
             :range :width
             :padding 0.05
             :round true}
            {:name :yscale
             :domain {:data :table
                      :field :amount}
             :nice true
             :range :height}]
   :axes [{:orient :bottom :scale :xscale}
          {:orient :left :scale :yscale}]
   :marks {:type :rect
           :from {:data :table}
           :encode {:enter {:x {:scale :xscale
                                :field :category}
                            :width {:scale :xscale
                                    :band 1}
                            :y {:scale :yscale
                                :field :amount}
                            :y2 {:scale :yscale
                                 :value 0}}
                    :update {:fill
                             {:value :steelblue}}
                    :hover {:fill
                            {:value :red}}}}}

  )

(def plotly-data
 (let [n 20
       walk (fn [bias]
              (->> (repeatedly n #(-> (rand)
                                      (- 0.5)
                                      (+ bias)))
                   (reductions +)))]
   {:data [{:x (walk 1)
            :y (walk -1)
            :z (map #(* % %)
                    (walk 2))
            :type :scatter3d
            :mode :lines+markers
            :opacity 0.2
            :line {:width 10}
            :marker {:size 20
                     :colorscale :Viridis}}]}))

(def people-as-maps
  (->> (range 29)
       (mapv (fn [_]
               {:preferred-language (["clojure" "clojurescript" "babashka"]
                                     (rand-int 3))
                :age (rand-int 100)}))))

(def people-as-vectors
  (->> people-as-maps
       (mapv (juxt :preferred-language :age))))

(def people-as-dataset
  (tc/dataset people-as-maps))

(defn fetch-dataset [dataset-name]
  (-> dataset-name
      (->> (format "https://vincentarelbundock.github.io/Rdatasets/csv/%s.csv"))
      (tc/dataset {:key-fn (fn [k]
                             (-> k
                                 str/lower-case
                                 (str/replace #"\." "-")
                                 keyword))})
      (tc/set-dataset-name dataset-name)))

(def iris
  (fetch-dataset "datasets/iris"))


(facts "eval works for different kinds"
       (k/kind-eval '(+ 1 1)) => {:html-data "2"}

       (k/kind-eval '(kind/md "# 123")) => {:markdown ["# 123"]}

       (str/starts-with?
        (-> (k/kind-eval '(kind/image image)) class (.getName))
        "clojupyter.misc.display$render_mime") => true

       (-> (k/kind-eval '[(kind/image image) (kind/image image)])
           :html-data
           (nth 3)
           (nth 2)
           (nth 2))
       => "nested rendering of :kind/image not possible in Clojupyter"


       (str/includes?
        (->
         (k/kind-eval  '^:kind/cytoscape cs)
         :html-data
         
         (nth 2)
         first
         second
         
         ) "cytoscape") => true)

(facts "options are checked"
       (str/starts-with? 
        (->
         (k/kind-eval '(kind/html "" {:invalid-option 1}) )
         :html-data
         (nth 2)
         
         )
        "invalid options"
        )
       
       )


(facts "kind/fn works as expected"
       (->
        
        (k/kind-eval  '(kind/fn {:x 1
                                 :y 2}
                         {:kindly/f (fn [{:keys [x y]}]
                                      (+ x y))}))
        :html-data
        
        )=> "3")

(facts "kind/table works"
       (->
        (k/kind-eval '(kind/table {:column-names [:a :b] :row-vectors [[1 2]]}))
        :html-data
        first) => :table


       (let [hiccup
             (k/kind-eval
              '(kind/table
                {:column-names [:preferred-language :age]
                 :row-vectors (take 5 people-as-vectors)}))]


         (-> hiccup :html-data second) => [:thead [:tr ":preferred-language" ":age"]]
         (-> hiccup :html-data (nth 2) count) => 6)
       
       

       ;https://github.com/scicloj/kindly-render/issues/29
       ;https://github.com/scicloj/kindly-render/issues/30

       ;; (k/kind-eval '(kind/table (take 5 people-as-vectors)))

       ;; (k/kind-eval '(kind/table (take 5 people-as-maps)))


       ;; (k/kind-eval '(kind/table {:x (range 6)
       ;;                            :y [:A :B :C :A :B :C]}))

       ;; (k/kind-eval '(-> people-as-maps
       ;;                   tc/dataset
       ;;                   (kind/table {:use-datatables true})))
       
       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true})))
       
       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   kind/table))
       
       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:element/max-height "300px"})))
       
       ;; (k/kind-eval '(-> people-as-maps
       ;;                   tc/dataset
       ;;                   (kind/table {:use-datatables true})))

       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true})))
       
       ;; (k/kind-eval '(-> people-as-dataset
       ;;                   (kind/table {:use-datatables true
       ;;                                :datatables {:scrollY 200}})))

       


       )



(facts "nil return nil"
       (k/kind-eval 'nil) => nil)


(facts "kind/map works"
       (k/kind-eval '(kind/map {:a 1})) 
       =>
       {:html-data
        [:div
         {:class "kind-map"}
         [:div {:style {:border "1px solid grey", :padding "2px"}} ":a"]
         [:div {:style {:border "1px solid grey", :padding "2px"}} "1"]]}
       
       (k/kind-eval '{:a 1})
       => {:html-data
           [:div
            {:class "kind-map"}
            [:div {:style {:border "1px solid grey", :padding "2px"}} ":a"]
            [:div {:style {:border "1px solid grey", :padding "2px"}} "1"]]})

(facts "kind/hidden returns nothing"
       (k/kind-eval
        '(kind/hidden "(+ 1 1)")) =>
       {:html-data nil})

(facts "kind/scittle works"
       (str/includes?
        (->
         (k/kind-eval '(kind/scittle '(.log js/console "hello")))
         :html-data
         (nth 3)
         )
        ".log js/console") => true

       (->
        (k/kind-eval '(kind/scittle '(print "hello")))
        :html-data
        (nth 3)) => [:script {:type "application/x-scittle", :class "kind-scittle"} "(print \"hello\")\n"]

       (->

        (k/kind-eval '(kind/scittle '(print "hello")))
        :html-data
        (nth 4)) => [:script "scittle.core.eval_script_tags()"])
       
(facts "kind/vega works"
       (str/starts-with? 
        (str (class (k/kind-eval '(kind/vega vega-spec))))
        "class clojupyter.misc.display$render_mime"

        ) => true)

(facts "kind/plotly works"
       (str/includes?
        (->
         (k/kind-eval '(kind/plotly plotly-data))
         :html-data
         (nth 2)
         first
         second)
        "Plotly.newPlot") => true

       (str/starts-with?
        (->
         (k/kind-eval '(kind/plotly plotly-data {:style {:width 100
                                                         :height 100}}))
         :html-data
         (nth 2)
         )
        "invalid options"
        ) => true
       
       )

(facts "kind/reagent works"
       (str/includes?
        (->
         (k/kind-eval 
          '(kind/reagent
            ['(fn [numbers]
                [:p {:style {:background "#d4ebe9"}}
                 (pr-str (map inc numbers))])
             (vec (range 10))]))
         :html-data
         second
         (nth 4)
         )
        "reagent.dom/render"
        )) => true

(facts "kind/reagent supports deps"
       (str/includes?
        (->
         (k/kind-eval
          '(kind/reagent
            ['(fn []
                [:div {:style {:height "200px"}
                       :ref (fn [el]
                              (let [m (-> js/L
                                          (.map el)
                                          (.setView (clj->js [51.505 -0.09])
                                                    13))]
                                (-> js/L
                                    .-tileLayer
                                    (.provider "OpenStreetMap.Mapnik")
                                    (.addTo m))
                                (-> js/L
                                    (.marker (clj->js [51.5 -0.09]))
                                    (.addTo m)
                                    (.bindPopup "A pretty CSS popup.<br> Easily customizable.")
                                    (.openPopup))))}])]
    ;; Note we need to mention the dependency:
            {:html/deps [:leaflet]}))
         :html-data
         second
         (nth 5))
        "leaflet.js") => true)

(facts "kind/image works"
       (str/starts-with?
        (->
         (k/kind-eval '(kind/image raw-image))
         class
         .getName
         )
        "clojupyter.misc.display$render_mime$reify"
        ))

(facts "nested image rendred as unsupported"
       (str/starts-with?
        (->
         (k/kind-eval
          '(kind/hiccup [:div.clay-limit-image-width
                         raw-image]))
         :html-data
         second
         (nth 2))
        "nested rendering of :kind/image not possible") => true
       (str/starts-with?
        (->
         (k/kind-eval
          '[raw-image raw-image])
         :html-data
         (nth 2)
         (nth 2)
         (nth 2))
        "nested rendering of :kind/image not possible")


       (str/starts-with?
        (->
         (k/kind-eval
          '[raw-image raw-image])
         :html-data
         (nth 2)
         (nth 2)
         (nth 2)
         )
        "nested rendering of :kind/image not possible"))

(facts "kind/fn works as expected "

       (->
        (k/kind-eval
         '(kind/fn
            {:kindly/f (fn [{:keys [x y]}]
                         (+ x y))
             :x 1
             :y 2}))
        :html-data) => "3"


       (->
        (k/kind-eval
         '(kind/fn
            {:x (range 3)
             :y (repeatedly 3 rand)}
            {:kindly/f tc/dataset}))
        :html-data
        (nth 2)) => [:p "_unnamed [3 2]:"]


       (-> '(kind/fn
              [+ 1 2])
           k/kind-eval
           :html-data) => "3"



       (-> '(kind/fn
              {:kindly/f tc/dataset
               :x (range 3)
               :y (repeatedly 3 rand)})

           k/kind-eval
           :html-data
           (nth 2)) => [:p "_unnamed [3 2]:"])


(facts "kind/var works"
       (-> '(kind/var '(def a 1))
           k/kind-eval
           :html-data)
       => "#'clojupyter.misc.kind-test/a")


;; Getting these pass would increase the "kind compatibility"

(facts "kind/pprint works"
       ;; bug: https://github.com/scicloj/kindly-render/issues/31
       ;; (->
       ;;  (to-hiccup/render {:form '(->> (range 30)
       ;;                                 (apply array-map)
       ;;                                 kind/pprint)})
       ;;  :hiccup
       ;;  (nth 2)
       ;;  second

       ;;  (nth 2))
       ;; => "{0 1,\n 2 3,\n 4 5,\n 6 7,\n 8 9,\n 10 11,\n 12 13,\n 14 15,\n 16 17,\n 18 19,\n 20 21,\n 22 23,\n 24 25,\n 26 27,\n 28 29}\n"
       )


(facts "kind/fragment works"

      ;;  (k/kind-eval
      ;;   '(->> ["purple" "darkgreen" "brown"]
      ;;         (mapcat (fn [color]
      ;;                   [(kind/md (str "### subsection: " color))
      ;;                    (kind/hiccup [:div {:style {:background-color color
      ;;                                                :color "lightgrey"}}
      ;;                                  [:big [:p color]]])]))
      ;;         kind/fragment))

      ;;  (k/kind-eval
      ;;   '(->> (range 3)
      ;;         kind/fragment))
       )

(facts "kind/code is working"

       ;;bug submitted: https://github.com/scicloj/kindly-render/issues/26
       ;(k/kind-eval '(kind/code "(defn f [x] {:y (+  x 9)})"))

       
       ;(kindly-advice/advise {:value (kind/code "(defn f [x] {:y (+  x 9)})")})

       ;(to-hiccup/render {:value (kind/code "(defn f [x] {:y (+  x 9)})")})
       ;;=> {:value ["(defn f [x] {:y (+  x 9)})"],
       ;;    :meta-kind :kind/code,
       ;;    :kindly/options {},
       ;;    :kind :kind/code,
       ;;    :advice [[:kind/code {:reason :metadata}] [:kind/vector {:reason :predicate}] [:kind/seq {:reason :predicate}]],
       ;;    :deps #{:kind/code},
       ;;    :hiccup [:pre {:class "kind-code"} [:code {:class "sourceCode"} nil]]}
       )

 





(facts "kind/video is working"
       ;bug report: https://github.com/scicloj/kindly-render/issues/27
       ;(k/kind-eval '(kind/video
       ;       {:youtube-id "DAQnvAgBma8"}
       )



(facts "kind/htmlwidgets-ggplotly is working"
       ;; (k/kind-eval
       ;;  '(kind/htmlwidgets-ggplotly {}))
       
       )

(facts "kind/edn is working"
       ;;  (k/kind-eval
       ;;   '(kind/edn {}))
       )

(facts "kind/smile-model is working"
       ;;   (k/kind-eval
       ;;    '(kind/smile-model {}))
       )



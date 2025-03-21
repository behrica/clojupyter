(ns clojupyter.misc.kind
  (:require
   [cheshire.core :as cheshire]
   [clojupyter.display :as display]
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [scicloj.kindly-render.note.to-hiccup :as to-hiccup]
   [scicloj.kindly-render.shared.walk :as walk]
   [scicloj.kindly-advice.v1.api :as kindly-advice]
   [clojure.string :as str])
  (:import
   [javax.imageio ImageIO]))

(defn require-js
  "Generates a Hiccup representation of a `<script>` tag that dynamically loads a JavaScript library from  
   a given URL and ensures that a specific JavaScript object provided by the library is available before  
   executing a rendering command. This is used to include external JavaScript libraries in a Jupyter notebook environment.  
  
   **Parameters:**  
  
   - `url` (String): The URL of the JavaScript library to load.  
   - `js-object` (String): The name of the JavaScript object that the library defines (e.g., `'Plotly'`, `'Highcharts'`).  
   - `render-cmd` (String): The JavaScript code to execute after the library has been loaded and the object is available.  
  
   **Returns:**  
  
   - A Hiccup vector representing a `<script>` tag containing JavaScript code that loads the library and executes `render-cmd` once the library is loaded."
  [url js-object render-cmd]
  [:script
   (format
    "  
    loadScript_%s = src => new Promise(resolve => {  
    script_%s = document.createElement('script');  
    script_%s.src = src;  
    script_%s.addEventListener('load', resolve);  
    document.head.appendChild(script_%s);  
  });  
  if (typeof %s === 'undefined') {    
     promise_%s=loadScript_%s('%s')  
       
     Promise.all([promise_%s]).then(() => {  
       console.log('%s loaded');  
        })  
       
     };  
 %s    
  
 "
    js-object js-object js-object
    js-object js-object js-object js-object
    js-object url js-object js-object
    render-cmd)])

(defn require-cytoscape
  "Creates a Hiccup representation to load the Cytoscape.js library and execute a rendering command after it has been loaded.  
  
   **Parameters:**  
  
   - `render-cmd` (String): The JavaScript command to execute after Cytoscape.js has been loaded.  
  
   **Returns:**  
  
   - A Hiccup vector that includes a `<script>` tag loading Cytoscape.js and executing the provided rendering command."
  [render-cmd]
  (require-js "https://cdnjs.cloudflare.com/ajax/libs/cytoscape/3.30.4/cytoscape.min.js"
              "cytoscape" render-cmd))

(defn require-plotly
  "Generates a Hiccup representation to load the Plotly.js library and execute a rendering command after it has been loaded.  
  
   **Parameters:**  
  
   - `render-cmd` (String): The JavaScript command to execute after Plotly.js has been loaded.  
  
   **Returns:**  
  
   - A Hiccup vector that includes a `<script>` tag loading Plotly.js and executing the provided rendering command."
  [render-cmd]
  (require-js "https://cdn.plot.ly/plotly-2.35.2.min.js"
              "Plotly"
              render-cmd))

(defn require-highcharts
  "Generates a Hiccup representation to load the Highcharts library and execute a rendering command after it has been loaded.  
  
   **Parameters:**  
  
   - `render-cmd` (String): The JavaScript command to execute after Highcharts has been loaded.  
  
   **Returns:**  
  
   - A Hiccup vector that includes a `<script>` tag loading Highcharts and executing the provided rendering command."
  [render-cmd]
  (require-js "https://code.highcharts.com/highcharts.js"
              "Highcharts" render-cmd))

(defn require-echarts
  "Creates a Hiccup representation to load the ECharts library and execute a rendering command after it has been loaded.  
  
   **Parameters:**  
  
   - `render-cmd` (String): The JavaScript command to execute after ECharts has been loaded.  
  
   **Returns:**  
  
   - A Hiccup vector that includes a `<script>` tag loading ECharts and executing the provided rendering command."
  [render-cmd]
  (require-js "https://cdn.jsdelivr.net/npm/echarts@5.4.1/dist/echarts.min.js"
              "echarts" render-cmd))

(defn highcharts->hiccup
  "Converts Highcharts chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the Highcharts library. It sets up a `<div>` container and includes the necessary scripts to render the chart.  
  
   **Parameters:**  
  
   - `value` (Map): The Highcharts configuration object representing the chart to render.  
  
   **Returns:**  
  
   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Highcharts chart with the provided configuration."
  [value]
  [:div {:style {:height "500px"
                 :width "500px"}}
   (require-highcharts (format "Highcharts.chart(document.currentScript.parentElement, %s);"
                               (cheshire/encode value)))])

(defn plotly->hiccup
  "Converts Plotly chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the Plotly library.  
  
   **Parameters:**  
  
   - `value` (Map): The Plotly configuration object representing the chart to render.  
  
   **Returns:**  
  
   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Plotly chart with the provided configuration."
  [value]
  [:div {:style {:height "500px"
                 :width "500px"}}
   (require-plotly (format "Plotly.newPlot(document.currentScript.parentElement, %s);"
                           (cheshire/encode value)))])

(defn cytoscape>hiccup
  "Converts Cytoscape graph data into a Hiccup vector that can render the graph within a Jupyter notebook using the Cytoscape.js library.  
  
   **Parameters:**  
  
   - `value` (Map): The Cytoscape.js configuration object representing the graph to render.  
  
   **Returns:**  
  
   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the Cytoscape graph with the provided configuration."
  [value]
  [:div {:style {:height "500px"
                 :width "500px"}}
   (require-cytoscape (format "  
                            {  
                            value = %s;  
                            value['container'] = document.currentScript.parentElement;  
                            cytoscape(value);  
                            };"
                              (cheshire/encode value)))])

(defn echarts->hiccup
  "Converts ECharts chart data into a Hiccup vector that can render the chart within a Jupyter notebook using the ECharts library.  
  
   **Parameters:**  
  
   - `value` (Map): The ECharts configuration object representing the chart to render.  
  
   **Returns:**  
  
   - A Hiccup vector containing a `<div>` with specified dimensions and a script that initializes the ECharts chart with the provided configuration."
  [value]
  [:div {:style {:height "500px"
                 :width "500px"}}
   (require-echarts (format "  
                                    {  
                                    var myChart = echarts.init(document.currentScript.parentElement);  
                                    myChart.setOption(%s);  
                                    };"
                            (cheshire/encode value)))])

(defn- default-to-hiccup-render
  "Provides a default rendering for notes by converting them into Hiccup format and preparing them for display in Clojupyter. It's a helper function used when no specific rendering method is available for a given kind.  
  
   **Parameters:**  
  
   - `note` (Map): A note containing information to render.  
  
   **Returns:**  
  
   - The `note` map augmented with `:clojupyter` and `:hiccup` keys, where `:clojupyter` contains the rendered HTML using `hiccup-html`, and `:hiccup` contains the Hiccup representation."
  [note]
  (let [hiccup
        (->
         (to-hiccup/render note)
         :hiccup)]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defn- render-non-nestable
  "Handles rendering for notes that cannot be nested within other renderings in Clojupyter. This function adds a message indicating that nested rendering is not possible for the given kind.  
  
   **Parameters:**  
  
   - `note` (Map): The note to render.  
   - `clojupyter` (Any): The rendering output suitable for direct display in Clojupyter.  
  
   **Returns:**  
  
   - The `note` map augmented with `:clojupyter` containing the rendering output, and `:hiccup` containing a message about the inability to nest the rendering."
  [note clojupyter]
  (assoc note
         :clojupyter clojupyter
         :hiccup [:div {:style "color:red"} (format "nested rendering of %s not possible in Clojupyter" (:kind note))]))

(defn- render-recursively
  "Recursively renders a data structure into Hiccup format, applying a rendering function to each element in the structure. It is used for rendering collections like vectors, maps, sets, and sequences.  
  
   **Parameters:**  
  
   - `note` (Map): The note to render.  
   - `value` (Collection): The data structure to render recursively.  
   - `css-class` (String): The CSS class to apply to the rendered elements.  
   - `render` (Function): The function to apply to each element for rendering.  
  
   **Returns:**  
  
   - The `note` map augmented with `:clojupyter` containing the rendered HTML, and `:hiccup` containing the Hiccup representation."
  [note value css-class render]
  (let [hiccup
        (:hiccup (walk/render-data-recursively note {:class css-class} value render))]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defn- render-table-recursively
  "Recursively renders table data structures into Hiccup format.  
  
   **Parameters:**  
  
   - `note` (Map): The note containing the table data.  
   - `render` (Function): The function to apply to each element for rendering.  
  
   **Returns:**  
  
   - The `note` map augmented with `:clojupyter` containing the rendered HTML, and `:hiccup` containing the Hiccup representation.  
  
   **Note:**  
  
   - There is a typo fixed in the implementation where `:hjccup` is corrected to `:hiccup`.  
   - There is a TODO to address any known issues."
  [note render]
  (let [hiccup
        ;; TODO: https://github.com/scicloj/kindly-render/issues/23  
        (:hjccup (walk/render-table-recursively note render))]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defn render-js
  "Renders JavaScript-based visualizations by converting the visualization data into Hiccup format and preparing it for display in Clojupyter.  
  
   **Parameters:**  
  
   - `note` (Map): The note containing the visualization data.  
   - `value` (Any): The data to render.  
   - `->hiccup-fn` (Function): A function that takes `value` and returns a Hiccup vector.  
  
   **Returns:**  
  
   - The `note` map augmented with `:clojupyter` containing the rendered HTML, and `:hiccup` containing the Hiccup representation."
  [note value ->hiccup-fn]
  (let [hiccup
        (->
         (->hiccup-fn value))]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defmulti render-advice :kind)

(defn render
  "Used to dispatch rendering to the appropriate `render-advice` method based on the `:kind` of the note.  
  
   **Parameters:**  
  
   - `note` (Map): The note containing the data and metadata to render.  
  
   **Returns:**  
  
   - The result of applying the appropriate `render-advice` method to the note."
  [note]
  (walk/advise-render-style note render-advice))

(defmethod render-advice :default
  [{:as note :keys [value kind]}]
  (let [hiccup (if kind
                 [:div
                  [:div "Unimplemented: " [:code (pr-str kind)]]
                  [:code (pr-str value)]]
                 (str value))]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defmethod render-advice :kind/plotly
  [{:as note :keys [value]}]
  (render-js note value plotly->hiccup))

(defmethod render-advice :kind/cytoscape
  [{:as note :keys [value]}]
  (render-js note value cytoscape>hiccup))

(defmethod render-advice :kind/highcharts
  [{:as note :keys [value]}]
  (render-js note value  highcharts->hiccup))

(defmethod render-advice :kind/echarts
  [{:as note :keys [value]}]
  (render-js note value echarts->hiccup))

(defmethod render-advice :kind/image
  [{:as note :keys [value]}]
  (let [out (io/java.io.ByteArrayOutputStream.)
        v (first value)
        clojupyter (do (ImageIO/write v "png" out)
                       (display/render-mime :image/png
                                            (-> out .toByteArray b64/encode String.)))]
    (render-non-nestable note clojupyter)))

(defmethod render-advice :kind/vega-lite
  [{:as note :keys [value]}]
  (render-non-nestable note (display/render-mime :application/vnd.vegalite.v3+json value)))

(defmethod render-advice :kind/md
  [{:as note :keys [value]}]
  (render-non-nestable note (display/markdown value)))

(defmethod render-advice :kind/tex
  [{:as note :keys [value]}]
  (render-non-nestable note (display/latex value)))

(defmethod render-advice :kind/dataset
  [note]
  (default-to-hiccup-render note))

(defmethod render-advice :kind/code
  [note]
  (default-to-hiccup-render note))

(defmethod render-advice :kind/pprint
  [note]
  (default-to-hiccup-render note))

(defmethod render-advice :kind/hidden
  [note]
  (default-to-hiccup-render note))

(defmethod render-advice :kind/video
  [note]
  (default-to-hiccup-render note))

(defmethod render-advice :kind/html
  [{:as note :keys [value]}]
  (assoc note
         :clojupyter (display/html (first value))
         :hiccup (first value)))

(defmethod render-advice :kind/hiccup
  [note]
  (let [hiccup
        (:hiccup (walk/render-hiccup-recursively note render))]
    (assoc note
           :clojupyter (display/hiccup-html hiccup)
           :hiccup hiccup)))

(defmethod render-advice :kind/vector
  [{:as note :keys [value]}]
  (render-recursively note value "kind-vector" render))

(defmethod render-advice :kind/map
  [{:as note :keys [value]}]
  (render-recursively note (apply concat value) "kind-map" render))

(defmethod render-advice :kind/set
  [{:as note :keys [value]}]
  (render-recursively note value "kind-set" render))

(defmethod render-advice :kind/seq
  [{:as note :keys [value]}]
  (render-recursively note value "kind-seq" render))

(defmethod render-advice :kind/table
  [note]
  (render-table-recursively note render))

(defmethod render-advice :kind/fn
  [{:keys [value form]}]
  (let [f (second (last value))
        note (render {:value (f value)
                      :form form})]

    (assoc note
           :hiccup (:hiccup note)
           :clojupyter (display/hiccup-html (:hiccup note)))))

(defn- render-as-clojupyter
  "Determines whether a given value should be rendered directly by Clojupyter without further processing. It checks if the value is `nil`, a known displayable type, or already a rendered MIME type.  
  
   **Parameters:**  
  
   - `form` (Any): The form that was evaluated.  
   - `value` (Any): The evaluated value of the form.  
  
   **Returns:**  
  
   - `true` if the value should be rendered directly by Clojupyter; `false` otherwise."
  [form value]
  (let [kindly-advice (kindly-advice/advise {:form form
                                             :value value})]
    (or
     (nil? value)
     (str/starts-with? (.getName (class value)) "clojupyter.misc.display$render_mime")
     (contains?
      #{clojupyter.misc.display.Latex
        clojupyter.misc.display.HiccupHTML
        clojupyter.misc.display.Markdown
        clojupyter.misc.display.HtmlString
        java.awt.image.BufferedImage}
      (class value)))))

(defn kind-eval
  "Evaluates a Clojure form and returns a value suitable for display in Clojupyter. If the evaluated value is of a type that Clojupyter can render directly, it is returned as-is. Otherwise, it applies custom rendering logic to prepare the value for display.  
  
   **Parameters:**  
  
   - `form` (Any): The Clojure form to evaluate.  
  
   **Returns:**  
  
   - The evaluated value if it's suitable for direct display.  
   - The custom-rendered content if additional processing is needed for display in Clojupyter.  
  
   **Behavior:**  
  
   1. Evaluates `form` to obtain `value`.  
   2. Uses `render-as-clojupyter` to determine if `value` can be displayed directly.  
   3. If `value` is suitable for direct display or is a Clojure var, it is returned.  
   4. Otherwise, it constructs a `note` with `:value` and `:form`, renders it with the `render` function, and returns the `:clojupyter` rendering."
  [form]
  ;(println :kind-eval--form form)  

  (let [value (eval form)]
    ;(println :kind-eval--value value)  
    ;(println :kind-eval--value-class (class value))  
    ;(println :kind-eval--advise kindly-advice)  
    (if (or (render-as-clojupyter form value)
            (var? value))
      value
      (:clojupyter (render {:value value
                            :form form})))))  
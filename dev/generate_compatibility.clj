(ns generate-compatibility
  (:require
   [cheshire.core :as json]
   [clojure.pprint :as pprint]
   [edamame.core :as e :refer [parse-string-all]]))

(def code-cells-setup
  [{:cell_type "code",
    :execution_count nil,
    :id "a63203c4-de7b-4481-be45-8d704963acfb",
    :metadata {},
    :outputs [],
    :source
    ["(require '[clojupyter.misc.helper :as helper])\n"
     "(def _ (helper/add-dependencies '[org.scicloj/noj \"2-beta5\"]))\n"
     "\n"
     " "]}
   
   {:cell_type "code",
    :execution_count nil,
    :id "f0d75d7b-287f-4e53-8b0b-f80d6cbef7be",
    :metadata {},
    :outputs [],
    :source
    ["(require '[scicloj.kindly.v4.kind :as kind]\n"
     "         '[tablecloth.api :as tc])"]}])


(defn create-code-cell [code]
  {:cell_type "code",
   :execution_count nil,
   :id (str (random-uuid))
   :metadata {},
   :outputs [],
   :source code})

(defn print-code [o]
  (binding [pprint/*print-right-margin* 100
            pprint/*print-miser-width* 60]
    (with-out-str
      (pprint/with-pprint-dispatch pprint/code-dispatch
        (pprint/pprint o)))))

(def parsed
  (->
   (slurp "https://raw.githubusercontent.com/scicloj/kindly-noted/refs/heads/main/notebooks/kinds.clj")
   (parse-string-all {:all true
                      :row-key :line
                      :col-key :column
                      :end-location false
                      :location? seq?})))

(def code-cells
  (map
   #(-> % print-code str create-code-cell)
   parsed))

(->> 
 {:cells
  (concat
   code-cells-setup
   code-cells)
  
  :metadata
  {:kernelspec
   {:display_name "Clojure (clojupyter-0.5.384-SNAPSHOT)",
    :language "clojure",
    :name "clojupyter-0.5.384-snapshot"},
   :language_info
   {:file_extension ".clj",
    :mimetype "text/x-clojure",
    :name "clojure",
    :version "1.12.0"}},
  :nbformat 4,
  :nbformat_minor 5}
 json/generate-string
 (spit "examples/kinds.ipynb" )
 )








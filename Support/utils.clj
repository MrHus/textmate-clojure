(in-ns 'textmate)
(clojure.core/refer 'clojure.core)
(require '[clojure.string :as string])
(require '[clojure.java.io :as io])

(defonce *compiled-files* (atom #{}))

(defn filepath->ns-str 
  "Convert filepath to ns-str"
  [path]
  (-> path
      (string/replace ".clj" "")
      (string/replace "_" "-")
      (string/replace "/" ".")))


(defn text-forms 
  "Wrap the forms in text t in a vector. Used
  for all the eval functions"
  [t]
  (read-string (str "[" t "]")))      


(defn file-ns 
  "Find the namespace of a file; searches for the first ns  (or in-ns) form
   in the file and returns that symbol. Defaults to 'user if one can't be found"
  []
  (let [forms (-> (bake/*env* "TM_FILEPATH") slurp text-forms)
        [ns-fn ns] (first (for [f forms :when (and (seq? f) (#{"ns" "in-ns"} (str (first f))))] 
                    [(first f) (second f)]))]                                                          
    (if ns  
      (if (= (str ns-fn) "ns") ns (eval ns))
      'user)))

      
(defn enter-ns 
  "Enter a ns, wrapped for debugging purposes"
  [ns]
  #_(println (str "Entering " ns))
  (in-ns ns))              


(defn enter-file-ns 
  "Enter the ns of the file"
  []
  (let [ns (file-ns)]
    (enter-ns ns)))     

    
(defmacro eval-in-file-ns 
  "For the current file, enter the ns (if any)
  and evaluate the form in that ns, then pop
  back up to the original ns"
  [& forms]
  `(let [old-ns# *ns*]
    (enter-file-ns)
    (let [r# ~@forms]
      (enter-ns (-> old-ns# str symbol))
      r#)))            

      
(defn project-relative-src-path []  
   (let [user-dir (str (bake/*env* "TM_PROJECT_DIRECTORY") "/src/")
        path-to-file (string/replace (bake/*env* "TM_FILEPATH")  user-dir "")]
  path-to-file))      
                   
(defn carret-info 
  "returns [path line-index column-index] info
   about current location of cursor"
  []
  [(bake/*env* "TM_FILEPATH")
    (dec (Integer/parseInt (bake/*env* "TM_LINE_NUMBER")))
    (Integer/parseInt (bake/*env* "TM_LINE_INDEX"))])      

(defn text-before-carret []
  (let [[path,line-index,column-index] (carret-info)
        lines (-> path io/reader line-seq)]
     (apply str 
       (apply str (for [l (take line-index lines)] (str l "\n")))
       (.substring #^String (nth lines line-index) 0 column-index))))    



(defn text-after-carret []
 (let [[path,line-index,column-index] (carret-info)
       lines (-> path io/reader line-seq)]
    (apply str 
      (.substring #^String (nth lines line-index) column-index)
      (apply str (for [l (drop (inc line-index) lines)] (str l "\n"))))))    
      
;(defn make-cannonical-form-text [t]
;  (.replaceAll #^String t "\\s+" " "))      
  
;(defn str-escape [t]
;  (.replaceAll #^String t "\\n" "\\n"))  
                
(defn get-last-sexpr 
  "Get last sexpr before carret"
  []
  (let [last (-> (text-before-carret) text-forms last)]
   #_(println "Last SEXPR" last)
   last))
  
(defn get-selected-sexpr 
  "Get highlighted sexpr"
  []
  (-> "TM_SELECTED_TEXT" bake/*env* clojure.core/read-string))
  
(defn get-enclosing-sexpr [])
(defn get-current-symbol [])
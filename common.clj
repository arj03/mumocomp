(ns common
  (:import (java.io BufferedReader InputStreamReader))
  (:import (java.io File))
)

(defn cmd-str [p] (.. Runtime getRuntime (exec (str p))))

; to support filenames with <space> in them stick is an array
(defn cmd 
  "Run a command. Supply an array to handle command with spaces."
  [p] (.. Runtime getRuntime (exec p)))

(defn cmdout
   "Pipe the output of a command to stdout"
   [o]
   (let [r (BufferedReader.
	    (InputStreamReader.
	     (.getInputStream o)))]
     (dorun (map println (line-seq r)))))

(defn do-on-thread
   "Create a new thread and run function f on it. Returns the thread object that
    was created."
   [f]
   (let [thread (new Thread f)]
     (.start thread)
     thread))

(defn run-simple-command-with-output [f a]
  (do-on-thread #(f (cmd (into-array a)))))

(defn run-simple-command [a]
  (cmd (into-array a)))

(defstruct db 
  :file 
  :data)

(defn write-db [db]
  (let [path (str (:file db) ".tmp")]
    (binding [*out* (java.io.FileWriter. path)]
      (prn (:data db)))
    (. (new File path) renameTo (new File (:file db)))))

(defn read-db [fname]
  (try
   (let [object (read-string (slurp fname))]
    (struct db fname object))
   (catch Exception e 
     (println (format "Caught exception when reading file %s:\n%s" fname e)))))

; lifo med timeout
(def timeout 1000) ; ms

; maps from name to agent and val, if agent is waiting to write
(def db-writers (ref {}))

(defn write-to-db [name val]
  (write-db (struct db (str global/web-folder (format "/%s.db" name)) val)))

(defn enqueue-write-db [name val]
  (dosync
   (let [db-writer-pair (@db-writers name)]
     (cond db-writer-pair
       (ref-set db-writers (assoc @db-writers name 
				  {:agent (:agent db-writer-pair) :val val}))
       :else
       (let [writer (agent {})]
	 (send writer (fn [s]
			(do 
			 (Thread/sleep timeout)
			 (dosync
			  (write-to-db name (:val (@db-writers name)))
			  (ref-set db-writers (dissoc @db-writers name))))))
	 (ref-set db-writers (assoc @db-writers name 
				    {:agent writer :val val})))))))

; convenience wrapper
(defn save-to-db [name]
  (fn [val]
    (enqueue-write-db name val)))

(defn load-from-db [name]
  (:data (read-db (str global/web-folder (format "/%s.db" name)))))

(defn eval-file-in-repl [file]
  (load-file (str global/web-folder file)))

; wrapper for adding watches where you don't care about anything
; except the new value
(defn value-ref-wrapper-watch [f]
  (fn [_ ref _ _]
    (f @ref)))

(defn ref-add [ref value]
  (dosync (ref-set ref (conj @ref value))))

(defn ref-upd [ref value]
  (dosync (ref-set ref value)))

(defn ref-upd-nil [ref value]
  (dosync (ref-set ref value))
  nil)

(defn find-first [filter-func seq]
  (first (filter filter-func seq)))

(defn upd-value [ref filter-func field new-value]
  (let [el (find-first filter-func @ref)
	rest (filter #(not (filter-func %)) @ref)]
    (when el
      (common/ref-upd ref (cons (merge el { field new-value }) rest))))
  nil)

(defn icompare [lhs rhs]
  (compare (. lhs toLowerCase ) (. rhs toLowerCase)))

(defn ieq [lhs rhs]
  (= (. lhs toLowerCase ) (. rhs toLowerCase)))

(defn nil-prune [seq]
  (filter #(not= % nil) seq))

(defn simple-cmp [name]
  #(icompare (name %1) (name %2)))

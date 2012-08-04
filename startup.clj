(ns common
  (:import (java.io BufferedReader InputStreamReader))
)

(defn eval-file-in-repl [file]
  (load-file (str "/home/arj/mumocomp/" file)))

(common/eval-file-in-repl "load.clj")

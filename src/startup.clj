(ns startup
  (:import (java.io BufferedReader InputStreamReader))
  (:gen-class)
)

(defn eval-file-in-repl [file]
  (load-file (str "/home/arj/mumocomp/" file)))

(defn -main [& args]
  (println "loading files")

  (eval-file-in-repl "global.clj")
  (eval-file-in-repl "common.clj")
  (eval-file-in-repl "screen-scraping.clj")
  (eval-file-in-repl "wikipedia.clj")
  (eval-file-in-repl "metal-archives.clj")
  (eval-file-in-repl "lastfm.clj")
  (eval-file-in-repl "playlist.clj")
  (eval-file-in-repl "audio.clj")
  (eval-file-in-repl "audio-player.clj")
  (eval-file-in-repl "long-polling.clj")

  ;movie stuff
  (eval-file-in-repl "imdb.clj")
  (eval-file-in-repl "rotten-tomatoes.clj")
  (eval-file-in-repl "movie.clj")

  (eval-file-in-repl "web-common.clj")
  (eval-file-in-repl "web-audio.clj")
  (eval-file-in-repl "web-movie.clj")
  (eval-file-in-repl "web-admin.clj")
  (eval-file-in-repl "web-start.clj")

  (println "done loading files :-)")
)

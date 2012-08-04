(ns common
  (:import (java.io BufferedReader InputStreamReader))
)

(defn eval-file-in-repl [file]
  (load-file (str "/home/arj/mumocomp/" file)))

;(common/eval-file-in-repl "load.clj")

(println "loading files")
(common/eval-file-in-repl "global.clj")
(common/eval-file-in-repl "common.clj")
(common/eval-file-in-repl "screen-scraping.clj")
(common/eval-file-in-repl "wikipedia.clj")
(common/eval-file-in-repl "metal-archives.clj")
(common/eval-file-in-repl "lastfm.clj")
(common/eval-file-in-repl "playlist.clj")
(common/eval-file-in-repl "audio.clj")
(common/eval-file-in-repl "audio-player.clj")
(common/eval-file-in-repl "long-polling.clj")

;movie stuff
(common/eval-file-in-repl "imdb.clj")
(common/eval-file-in-repl "rotten-tomatoes.clj")
(common/eval-file-in-repl "movie.clj")

(common/eval-file-in-repl "web-common.clj")
(common/eval-file-in-repl "web-audio.clj")
(common/eval-file-in-repl "web-movie.clj")
(common/eval-file-in-repl "web-admin.clj")
(common/eval-file-in-repl "web-start.clj")

(println "done loading files :-)")

(ns lastfm
  (:import (de.umass.lastfm User Artist Album Track ImageSize Session Authenticator))
  (:import (de.umass.lastfm.scrobble ScrobbleData Source Rating ResponseStatus))
)

(def api_key "e6c81e1ab10cacec97bf393394ae47ba")
(def api_secret "2649692ddad0731e381cc63bc0e0b2ac")

; define your username and password in personal.clj
(def user "")
(def password "")
;(common/eval-file-in-repl "personal.clj")

(defn get-session []
  (try
   (. Authenticator getMobileSession 
      user password api_key api_secret)
   (catch Exception e
     (println "failed to get last.fm session"))))

(def session (ref (get-session)))

(defn get-album [artist album]
  (. Album getInfo artist album api_key))

(defn artist-top-tags [artist]
  (map #(. % getName) (. Artist getTopTags artist api_key)))

(defn get-loved-tracks [page]
  (. User getLovedTracks user page api_key))

(defn get-loved-tracks-pagecount []
  (. (get-loved-tracks 1) getTotalPages))

; FIXME: handle exception
(defn session-wrapped [func]
  (let [status (func)]
    (when (or 
	   (= (. status getStatus) ResponseStatus/BADSESSION)
	   (= (. status getStatus) ResponseStatus/FAILED))
      (common/ref-upd session (get-session)))))

(defn love-track [artist title]
  (session-wrapped #(. Track love artist title @session)))

(defn now-playing [artist album title duration]
  (session-wrapped #(. Track updateNowPlaying (new ScrobbleData artist title (/ (System/currentTimeMillis) 1000) duration album "" "" 0 "") @session)))

(defn scrobble [track]
  (session-wrapped 
   #(. Track scrobble (:artist track) (:title track)
       (/ (System/currentTimeMillis) 1000) @session)))

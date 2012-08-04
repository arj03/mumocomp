(ns audio
  (:import (de.umass.lastfm ImageSize))
  (:import (org.jaudiotagger.audio AudioFileIO AudioHeader))
  (:import (org.jaudiotagger.tag FieldKey))
  (:import (java.io File))
  (:import [java.util.logging LogManager]
           [java.io StringBufferInputStream])
  (:require [clojure.string :as string])
)

; disable jaudiotagger's crazy logging
(let [stream (StringBufferInputStream. "org.jaudiotagger.level = OFF")]
  (.. (LogManager/getLogManager)
      (readConfiguration stream)))

(defstruct Track
  :filename
  :artist
  :album
  :title
  :length ; in seconds
  :album-track
  :loved
)

(defstruct Artist
  :name
  :genres ; array of genres
  :top-tracks ; array of Tracks
  :nr-plays
)

(defstruct Album
  :name
  :artist
  :year
  :cover
  :large-cover
  :wikipedia
  :metal-archives
  :ma-rating
)

(defstruct Folder
  :name
  :folder
)

(def files (ref []))
(def tracks (ref {}))
(def artists (ref #{}))
(def albums (ref []))

; tracks in folder
; (filter #(= (. (first %) indexOf (. folder getPath)) 0) @tracks)

(defn traverse-filesystem [root files]
  (cond (. root isDirectory)
    (doseq [f (. root list)]
      (traverse-filesystem (new File root f) files))
    :else (common/ref-add files root)))

(defn filter-filename [file]
  (let [lpath (.toLowerCase (. file getPath))]
    (some #(. lpath endsWith %) global/valid-extensions-a)))

(defn read-files []
  (doseq [folder global/music-folders]
    (traverse-filesystem (new File folder) files)))

(defn filter-files []
  (common/ref-upd-nil files (filter filter-filename @files)))

(defn extract-track-nr [tags]
  (try
   (let [track (. tags getFirst FieldKey/TRACK)]
     (cond (= (. track indexOf "/") 0)
	   (new Integer track)
	   :else
	   (new Integer (. track substring (+ (. track indexOf "/") 1)))))
   (catch Exception e 
     0)))

(defn create-track-from-file [file]
  (try 
   (let [afio (. AudioFileIO read file)
	 tags (. afio getTag)]
     (struct Track
	     (. file getPath)
	     (. tags getFirst FieldKey/ARTIST)
	     (. tags getFirst FieldKey/ALBUM)
	     (. tags getFirst FieldKey/TITLE)
	     (.. afio getAudioHeader getTrackLength)
	     (extract-track-nr tags)
	     false))
   (catch Exception e
    (. *err* print 
       (format "Caught exception when converting file %s:\n%s\n" 
	       (. file getName) e)))))

(defn generate-tracks []
  (let [tmp (filter #(not= % nil) (map create-track-from-file @files))]
    (common/ref-upd-nil tracks (zipmap (map :filename tmp) tmp))))

(defn first-run []
  (read-files)
  (filter-files)
  (generate-tracks))

; suck out all artists and populate them

(defn add-artist-to-artists [artist]
  (common/ref-add artists artist))

(defn get-artists-from-tracks []
  (map #(struct Artist %) (set (map :artist (vals @tracks)))))

(defn set-artists []
  (common/ref-upd-nil artists (get-artists-from-tracks)))

(defn set-artist-genres [artist]
  (when (string? (artist :name))
    (try
     (merge artist { :genres (take 3 (lastfm/artist-top-tags (artist :name))) })
     (catch Exception e 
       (println "failed to fetch artist tags on last.fm")
       artist))))

(defn set-all-artist-genres []
  (common/ref-upd-nil artists 
		      (for [artist @artists] (set-artist-genres artist))))

(defn create-album [artist album]
  (try
   (let [lastfm-album (lastfm/get-album artist album)]
     (when lastfm-album
       (struct Album album artist
	       (let [d (. lastfm-album getReleaseDate)]
		 (cond d (+ (. d getYear) 1900)
		       :else 0))
	       (. lastfm-album getImageURL ImageSize/MEDIUM)
	       (. lastfm-album getImageURL ImageSize/LARGE))))
   (catch Exception e
     (println "failed to fetch album on last.fm")
     (struct Album album artist))))

(defn update-albums []
  (let [amap (set (map #(hash-map :artist (:artist %) :album (:album %)) (vals @tracks)))]
    (common/ref-upd-nil albums (map #(create-album (:artist %) (:album %)) amap))))

(defn add-wikipedia-reference [album]
  (let [fixed-album (wikipedia/get-published-year (:artist album) (:name album))]
    (cond (and fixed-album (not= (:year fixed-album) 0))
	  (assoc album :year (:year fixed-album) :wikipedia (:url fixed-album))
	  :else album)))

(defn add-wikipedia-to-albums []
  (common/ref-upd-nil albums (map #(cond (= (:wikipedia %) nil)
					 (do
					   (Thread/sleep (rand 10000)) 
					   (add-wikipedia-reference %))
					 :else %) @albums)))

(defn add-metal-archives-reference [album]
  (let [fixed-album (metal-archives/get-info (:artist album) (:name album))]
    (cond (and fixed-album (not= (:year fixed-album) 0))
	  (assoc album :year (:year fixed-album) :metal-archives (:url fixed-album) :ma-rating (:rating fixed-album))
	  :else album)))

(defn update-album-online-references [album-name]
  (let [album-filter #(= (:name %) album-name)
	album (first (filter album-filter @albums))
	fixed-album (add-metal-archives-reference (add-wikipedia-reference album))
	other-albums (filter #(not (album-filter %)) @albums)]
    (println "updating " album-name)
    (common/ref-upd-nil albums (conj other-albums fixed-album))))

(defn add-metal-archives-to-albums []
  (common/ref-upd-nil albums (map #(cond (= (:wikipedia %) nil)
					 (do
					   (Thread/sleep (rand 10000)) 
					   (add-metal-archives-reference %))
					 :else %) @albums)))

(defn get-album-cover [artist-name album-name]
  (let [album (filter #(and (= (:name %) album-name) (= (:artist %) artist-name)) @albums)]
    (cond (> (count album) 0) 
	  (:cover (first album))
	  :else "")))

; FIXME: not working, needs updated library
;(defn set-artist-play-count [artist]
;  (if (string? (artist :name))
;    (merge artist { :play-count (. Artist getInfo (artist :name) api_key)) }))

; inotify related functions

(defn create-artist-from-track [t]
  (struct Artist (:artist t)))

(defn add-file [filename]
  (let [file (new File filename)]
    (when (filter-filename file)
      (let [t (create-track-from-file file)]
	(when t
	  (dosync
	   (when (not-any? #(= (:artist t) (:name %)) @artists)
	     (let [a (create-artist-from-track t)]
	       (add-artist-to-artists (set-artist-genres a)))))
	  (let [a (add-metal-archives-reference 
		   (add-wikipedia-reference 
		    (create-album (:artist t) (:album t))))]
	    (dosync
	     (when (not-any? #(and 
			       (= (:album t) (:name %)) 
			       (= (:artist t) (:artist %))) @albums)
	       (common/ref-add albums a))))
	  (println (:filename t))
	  (common/ref-upd-nil tracks (assoc @tracks (:filename t) t)))))))

(defn add-file-with-check [file]
  (when (filter-filename file)
    (when (not (contains? @tracks (. file getPath)))
      (let [t (create-track-from-file file)]
        (when t
          (dosync
           (when (not-any? #(= (:artist t) (:name %)) @artists)
             (let [a (create-artist-from-track t)]
               (add-artist-to-artists (set-artist-genres a))))
           (when (not-any? #(and 
                             (= (:album t) (:name %)) 
                             (= (:artist t) (:artist %))) @albums)
             (let [a (add-metal-archives-reference 
                      (add-wikipedia-reference 
                       (create-album (:artist t) (:album t))))]
               (Thread/sleep (rand 10000))
               (common/ref-add albums a))))
          (println (:filename t))
          (common/ref-upd-nil tracks (assoc @tracks (:filename t) t)))))))

(defn traverse-filesystem-adding-files [root files]
  (cond (. root isDirectory)
    (doseq [f (. root list)]
      (traverse-filesystem-adding-files (new File root f) files))
    :else (add-file-with-check root)))

(defn add-new-files []
  (doseq [folder global/music-folders]
    (traverse-filesystem-adding-files (new File folder) files)))

(defn populate-tracks-with-love [])

(defn new-folder [folder]
  (let [newfiles (ref [])]
    (traverse-filesystem (new File folder) newfiles)
    (doseq [file @newfiles] (add-file (. file getPath)))
    (populate-tracks-with-love)))

(defn remove-file [filename]
  (let [file (new File filename)]
    (when (filter-filename file)
      (let [track (get @tracks filename)
	    artist (:artist track)
	    album (:album track)]
	(dosync 
	 (ref-set tracks (dissoc @tracks filename))
	 (when (not-any? #(= (:artist %) artist) @tracks)
	   (ref-set artists (doall (remove #(= (:name %) artist) @artists))))
	 (when (not-any? #(and (= (:album %) album) (= (:artist %) artist)) @tracks)
	   (ref-set albums (doall (remove #(and (= (:name %) album) (= (:artist %) artist)) @albums)))
	   ))
      )))
  nil)

(defn first-track-in-album [artist-name album-name]
  (common/find-first #(and (= (:album %) album-name) (= (:artist %) artist-name)) (vals @tracks)))

(defn remove-album [album]
  (common/ref-upd-nil albums (doall (remove #(= (:name %) (:name album)) @albums))))

(defn cleanup-albums []
  (doall (map #(when (= (first-track-in-album (:artist %) (:name %)) nil) (remove-album %)) @albums))
  nil)

(defn first-track-of-artist [artist-name]
  (common/find-first #(= (:artist %) artist-name) (vals @tracks)))

(defn remove-artist [artist]
  (common/ref-upd-nil artists (doall (remove #(= (:name %) (:name artist)) @artists))))

(defn cleanup-artists []
  (doall (map #(when (= (first-track-of-artist (:name %)) nil) (remove-artist %)) @artists))
  nil)

(defn remove-folder [folder]
  (let [track (common/find-first #(.contains % (str folder "/")) (keys @tracks))
	artist (:artist track)
	album (:album track)]
    (dosync
     (ref-set tracks (apply dissoc @tracks (filter #(.contains % (str folder "/")) (keys @tracks)))))
    (cleanup-artists)
    (cleanup-albums))
  nil)

; add some love

; FIXME: maybe do some caching?

(defn get-loved-tracks []
  (let [pagecount (lastfm/get-loved-tracks-pagecount)]
    (for [page (range 1 (+ pagecount 1))]
      (seq (. (lastfm/get-loved-tracks page) getPageResults)))))

(defn find-track [artist title]
  (common/find-first #(and (common/ieq (:title %) title) (common/ieq (:artist %) artist)) (vals @tracks)))

(defn love-track [artist title]
  (let [track (find-track artist title)]
    (when track
      (common/ref-upd-nil tracks
        (assoc @tracks (:filename track) (assoc track :loved true))))))

(defn populate-tracks-with-love []
  (let [loved-tracks (flatten (get-loved-tracks))]
    (map #(love-track (. % getArtist) (. % getName)) loved-tracks))
  nil)

; helper functions

(defn get-album-from-track [track]
  (common/find-first #(= (:album track) (:name %)) @albums))

(defn sort-tracks [track1 track2]
  (cond (= (:album track1) (:album track2))
    (cond (and (:album-track track1) (:album-track track2))
      (< (:album-track track1) (:album-track track2))
      :else 0)
    :else
     (> (:year (get-album-from-track track1)) (:year (get-album-from-track track2)))))

(defn sort-albums [album1 album2]
  (let [a1 (common/find-first #(= (:name %) album1) @albums)
	a2 (common/find-first #(= (:name %) album2) @albums)]
    (cond (and a1 a2) (> (:year a1) (:year a2))
	  :else 1)))

(defn tracks-by-genre [genre]
  (let [artists (map :name (filter #(some #{ genre } (:genres %)) @artists))]
    (sort sort-tracks (filter #(some #{(:artist %)} artists) (vals @tracks)))))

(defn tracks-by-artist [artist]
  (sort sort-tracks (filter #(= (:artist %) artist) (vals @tracks))))

(defn tracks-by-album [album]
  (sort sort-tracks (filter #(= (:album %) album) (vals @tracks))))

; formatting

(defn format-track [track]
  (format "%s - %s  - %s" (:artist track) (:album track) (:title track)))

(defn format-track-simple [track]
  (format "%s" (:title track)))

(defn format-length [length]
  (format "%d:%02d" (quot length 60) (rem length 60)))

; save: set hooks that write to disc on changes

(add-watch tracks "save" (common/value-ref-wrapper-watch (common/save-to-db "tracks")))
(add-watch artists "save" (common/value-ref-wrapper-watch (common/save-to-db "artists")))
(add-watch albums "save" (common/value-ref-wrapper-watch (common/save-to-db "albums")))

; load

(common/ref-upd-nil tracks (common/load-from-db "tracks"))
(common/ref-upd-nil artists (common/load-from-db "artists"))
(common/ref-upd-nil albums (common/load-from-db "albums"))

; other examples

;(.getWatches audio/artists)

;(sort-by :album-track t)

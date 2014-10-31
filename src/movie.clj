(ns movie
  (:import (java.io File))
  (:import (System))
  (:require [clojure.string :as string])
)

(defstruct Movie
  :path
  :name
  :last-watched
  :modified
)

(defstruct Folder
  :path
  :name
)

(defstruct MovieInfo
  :path
  :year
  :imdb-rating
  :imdb-url
  :image
  :rotten-tomatoes-rating
  :rotten-tomatoes-url
)

(def movies (ref {}))
(def movie-infos (ref {}))

(defn create-movie [file]
  (struct Movie
	  (. file getPath)
	  (. file getName)
	  nil
	  (. file lastModified)))

(defn extension [path]
  (. path substring (. path lastIndexOf ".")))

(defn filter-filename [file]
  (let [lpath (.toLowerCase (. file getPath))]
    (some #(. lpath endsWith %) global/valid-extensions-m)))

(defn is-internet-info-folder [path]
  (not= nil (common/find-first #(= (. % substring 0 (. % lastIndexOf "/")) path) (keys @movie-infos))))

(defn create-entry [path]
  (let [file (File. path)
        movie (common/find-first #(= (:path %) (. file getPath)) (vals @movies))]
    (cond (not= movie nil) movie
	  :else
	  (when (. file isDirectory)
	    (struct Folder (. file getPath) (. file getName))))))

(defn list-folder [path]
  (let [folder (File. path)]
    (remove nil? (map #(create-entry (str (. folder getPath) "/" %)) (. folder list)))))

(defn generate-movies [root]
  (doseq [f (. root list)]
    (let [file (File. root f)]
      (cond (. file isDirectory)
	    (generate-movies file)
	    :else
	    (when (filter-filename file)
	      (when (not (@movies (. file getPath)))
		(let [movie (create-movie file)]
		  (common/ref-upd-nil movies (assoc @movies (:path movie) movie)))))))))

(defn read-files []
  (doseq [f global/movie-folders]
    (generate-movies (File. f))))

(read-files)

; FIXME: this is not saved persistently
(defn update-last-watched [path]
  (let [movie (@movies path)]
    (common/ref-upd-nil movies (assoc @movies (:path movie)
      (assoc movie :last-watched (. System currentTimeMillis))))))

(def strip-sections ["720p" "1080p" "1080" "Directors" "Cut" "Unrated" "Limited" "Extended" "REMASTERED" "EDiTiON" "EXTENDED" "CUT" "PROPER" "AC3" "AC3D" "DTS" "DTheater" "DL" "DC" "SE" "MULTISUBS" "BlueRay" "BluRay" "Bluray" "BD5" "BD9" "DVD5" "DVD9" "HDDVDRip" "BDRip" "x264" "SEPTiC" "CYBERMEN" "LiMiTED" "SiNNERS" "BDFLiX" "CiNEFiLE" "HDTV" "HDDVD" "hV" "BestHD" "iNFAMOUS" "REFiNED" "IGUANA" "HANGOVER" "RAP" "DOWN" "HDCLASSiCS" "CBGB" "BDiSC" "HDEX" "CDDHD" "METiS" "TiMELORDS" "RETREAT" "CYBERMEN" "PROGRESS" "LIMITED" "OAR" "TQF" "NBS" "REVEiLLE" "4HM" "mVmHD" "DEFiNiTE" "BoNE" "HDT" "LsE" "Chakra" "WPi" "THOR" "aAF" "avi" "mkv" "mpg" "iNT" "LCHD" "X264" "AMIABLE" "FLHD" "MaxHD" "MELiTE" "UNRATED" "SECTOR7" "REPACK" "BLOW" "Felony" "MHD" "RETAIL"])

(defn fix-name [name]
  (let [split-str (string/split (string/replace name ".5.1" ".") #"\.|-")
	fixed-split-str (filter #(and 
				  (not (some #{%} strip-sections))
				  (not (re-find #"^\d\d\d\d$" %))) split-str)]
    (apply str (reduce #(format "%s %s" %1 %2) fixed-split-str))))

(defn get-movie-info-file [file]
  (let [f (. file getName)
        fixed-f (fix-name f)
        imdb-info (imdb/get-rating fixed-f)
        rotten-info (rotten/get-rating fixed-f)]
    ; imdb doesn't allow linking to their images directly, so we only use rotten
    {(. file getPath) (struct MovieInfo (. file getPath) (:year imdb-info) (:rating imdb-info) (:url imdb-info) (:img rotten-info) (:rating rotten-info) (:url rotten-info))}))

(defn get-movie-info [folder f]
  (let [file (File. folder f)]
    (get-movie-info-file file)))

(defn update-movie-info [folder]
  (doseq [f (. (File. folder) list)]
    (cond (not (movie-infos (. (File. folder f) getPath)))
	  (do
	    (println (fix-name f))
	    (Thread/sleep (rand 10000))
	    (println "slept")
	    (try
	     (do
	       (common/ref-add movie-infos (get-movie-info folder f))
	       (println "added"))
	     (catch Exception e
	       (println (. e getMessage))
	       (println "-failed"))))
	  :else (println f " already exists"))))

(defn update-movie [path]
  (common/ref-upd-nil movie-infos (assoc @movie-infos path ((get-movie-info-file (File. path)) path))))

(defn update-movie [folder file]
  (let [path (str folder file)]
    (common/ref-upd-nil movie-infos (assoc @movie-infos path ((get-movie-info folder file) path)))))

(add-watch movie-infos "save" (common/value-ref-wrapper-watch (common/save-to-db "movie-infos")))

(common/ref-upd-nil movie-infos (common/load-from-db "movie-infos"))

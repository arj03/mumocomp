; command to start alsaplayer from command line:
; sudo alsaplayer -i daemon -r --nosave

; this file depends on playlist.clj

(ns audio-player
  (:import (java.io BufferedReader InputStreamReader))
)

(def scrobble-min-time 20)

; a macro that helps write better agent code by catching if a function
; doesn't return anything, and then returning the state unmodified.

(defmacro defa [name args & form]
  `(defn ~name [~@args]
     (let [result# (do ~@form)]
       (cond (not result#)
            (first ~args)
            :else
            result#))))

; last.fm

(import '(java.util.concurrent BlockingQueue LinkedBlockingQueue TimeUnit))

(def failed-tracks (ref (new LinkedBlockingQueue)))

(defn failed-agent [agent exception]
  (println "Agent failed:")
  (println (.getMessage exception))
  (println (.printStackTrace exception)))

(def pollers-queue (agent {:failures 0} :error-handler failed-agent))

(defn call-last-fm [data]
  (let [type (:type data)]
    (cond (= type "now-playing")
	  (lastfm/now-playing (:artist data) (:album data) (:title data) (:length data))
	  (= type "scrobble")
	  (lastfm/scrobble (:track data))
	  (= type "love-track")
	  (lastfm/love-track (:artist data) (:title data)))))

(defn check-queue [cur-state]
  (let [failed-track (. @failed-tracks take)]
    (try
     (call-last-fm failed-track)
     true
     (catch Exception e
       (. @failed-tracks put failed-track)
       (println (.getMessage e))
       (println (.printStackTrace e))
       (println "failed to call last.fm")
       false))))

(defn loop-scrobble-pool [state]
  (println "checking queue")
  (let [q-status (check-queue state)]
    (when-not q-status
      (Thread/sleep (* (. java.lang.Math (pow 2 (:failures state))) 1000)))
    (send pollers-queue loop-scrobble-pool)
    (cond q-status (assoc state :failures 0)
	  :else 
	  (do
	    (cond (< (:failures state) 5)
		  (assoc state :failures (inc (:failures state)))
		  :else 
		  state)))))

(send pollers-queue loop-scrobble-pool)

(defn queue-abstraction [data]
  (try
   (call-last-fm data)
   (catch Exception e
     (println "failed to call last.fm")
     (. @failed-tracks put data))))

; FIXME: not working
;(add-watch failed-tracks "save" (common/value-ref-wrapper-watch (common/save-to-db "last.fm")))
;(common/ref-upd failed-tracks (common/load-from-db "last.fm"))

(def scrobbling (agent {:started-playing 0
			:cur-track nil
			:loved-track false}
		        :error-handler failed-agent))

(defn now-playing-on-agent [cur-state track]
  (let [artist (:artist track)
	album (:album track)
	title (:title track)
	length (:length track)]
    (println "scrobbling now playing!")
    (queue-abstraction { :type "now-playing" :artist artist :album album :title title :length length})
    (assoc cur-state :cur-track track :started-playing (/ (System/currentTimeMillis) 1000))))

(defn scrobble-on-agent [cur-state]
  (println "looking to scrobble")
  (let [started-playing (cur-state :started-playing)
	playing-time (- (/ (System/currentTimeMillis) 1000) started-playing)
	track (cur-state :cur-track)]
    (when (and track (> playing-time scrobble-min-time))
      (println "scrobbling " (:filename track))
      (queue-abstraction { :type "scrobble" :track track })))
    (assoc cur-state :loved-track false :cur-track nil))

(defn scrobble []
  (send scrobbling scrobble-on-agent))

(defa love-track-on-agent [cur-state]
  (let [scrobble-track (cur-state :cur-track)]
    (when scrobble-track
      (println "loving track")
      (try
       (let [artist (:artist scrobble-track)
	     title (:title scrobble-track)]
	 (queue-abstraction {:type "love-track" :artist artist :title title })
	 (audio/love-track artist title)
	 (playlist/love-track artist title)
	 (assoc cur-state :loved-track true))
       (catch Exception e
	 (println "failed to love track"))))))

(defn love-track []
  (send scrobbling love-track-on-agent))

(def state (agent {:state :stopped
		   :player-position 0
		   :playlist-index 0}
		  :error-handler failed-agent))

(defn is-playing []
  (not= (@state :state) :stopped))

(defn is-paused []
  (= (@state :state) :paused))

(defn playlist-index []
  (@state :playlist-index))

; forward decleration
(defn cmd-with-update [vec])

(defn can-advance-to-next-track [cur-state]
  (< (inc (cur-state :playlist-index)) (count @playlist/tracks)))

(defn extract-playback-position [msg]
  (let [first-index (inc (. msg indexOf ":"))
	second-index (inc (. msg indexOf ":" first-index))
	minutes (new Integer (. (. msg substring first-index (. msg indexOf ":" first-index)) replace " " ""))
	seconds (new Integer (. msg substring second-index (+ second-index 2)))]
    (+ (* 60 minutes) seconds)))

(defn end-of-playback [cur-state]
  (assoc cur-state :state :stopped :player-position 0))

; forward decleration
(defn next-track-on-agent [cur-state])

(defa process-message-on-agent [cur-state msg]
  (cond (. msg startsWith "Playing") 
	(assoc cur-state :player-position (extract-playback-position msg))
	(= msg "...done playing") 
	(do (scrobble)
	    (cond (can-advance-to-next-track cur-state)
		  (next-track-on-agent cur-state)
		  :else
		  (end-of-playback cur-state)))))

(defn filter-msg [o]
  (let [r (BufferedReader.
	   (InputStreamReader.
	    (.getInputStream o)))]
    (dorun (map #(when-not (is-paused)
		   (send state process-message-on-agent %)) (line-seq r)))))

(defn cmd-with-update [vec]
  (common/run-simple-command-with-output filter-msg vec))

; commands

(defn kill-player []
  (common/run-simple-command (vector "killall" "-9" "alsaplayer")))

(defn play-track-simple [cur-state track]
  (println (format "playing simple tracks %s" (:filename track)))
  (when (not= (cur-state :state) :stopped)
    (. (kill-player) waitFor)
    (send state #(assoc %1 :player-position 0)))
  (cmd-with-update (vector "alsaplayer" "-i" "text" "-E" (:filename track))))

(defn play-track-i-on-agent [cur-state i]
  (play-track-simple cur-state (nth @playlist/tracks i))
  (send scrobbling now-playing-on-agent (nth @playlist/tracks i))
  (assoc cur-state :state :playing :playlist-index i))

(defn play-track-i [i]
   (when (< i (count @playlist/tracks))
     (send state play-track-i-on-agent i)))

(defn pause-on-agent [cur-state]
  (cond (not= (cur-state :state) :paused)
	(do 
	  (common/run-simple-command (vector "alsaplayer" "-i" "text" "--pause"))
	  (assoc cur-state :state :paused))
	:else
	(do 
	  (common/run-simple-command (vector "alsaplayer" "-i" "text" "--start"))
	  (assoc cur-state :state :playing))))
  
(defn pause []
  (send state pause-on-agent))

(defa play-track-on-agent [cur-state]
  (let [pindex (cur-state :playlist-index)]
    (when (< pindex (count @playlist/tracks))
      (println (format "playing track %d" pindex))
      (play-track-i-on-agent cur-state pindex))))

(defn play []
  (send state play-track-on-agent))

(defa next-track-on-agent [cur-state]
  (let [pindex (inc (cur-state :playlist-index))]
    (when (< pindex (count @playlist/tracks))
      (println (format "skipping to next track %d" pindex))
      (play-track-i-on-agent cur-state pindex))))

(defn next-track []
  (send state next-track-on-agent))

(defa prev-track-on-agent [cur-state]
  (let [pindex (cur-state :playlist-index)]
    (when (> pindex 0)
      (play-track-i-on-agent cur-state (dec pindex)))))

(defn prev-track []
  (send state prev-track-on-agent))

(defn stop-on-agent [cur-state]
  (. (kill-player) waitFor)
  (end-of-playback cur-state))

(defn stop []
  (send state stop-on-agent))

; playlist signals

(defa track-removed-on-agent [cur-state index]
  (let [pindex (cur-state :playlist-index)]
    (cond (= pindex index)
	  (cond 
	    (= (count @playlist/tracks) 0) (stop-on-agent cur-state)
	    (>= pindex (count @playlist/tracks))
	    (let [new-index (dec pindex)]
	      (cond (= (cur-state :state) :playing)
		    (play-track-i-on-agent cur-state new-index)
		    :else
		    (assoc cur-state :playlist-index new-index)))
	    :else 
	    (when (= (cur-state :state) :playing)
		  (play-track-i-on-agent cur-state pindex)))
	  (> pindex index)
	  (assoc cur-state :playlist-index (dec pindex)))))

(defmethod playlist/track-removed :default [index]
  (send state track-removed-on-agent index))

(defmethod playlist/all-tracks-removed :default [_]
  (send state #(assoc (stop-on-agent %) :playlist-index 0)))

(ns inotify
  (:import (net.contentobjects.jnotify JNotify JNotifyListener))
)

;(def mask JNotify/FILE_ANY)
(def mask (bit-or (bit-or (bit-or JNotify/FILE_CREATED JNotify/FILE_DELETED) JNotify/FILE_RENAMED) JNotify/TREAT_MOVE_AS_DELETE_CREATE))

(def watchIDs (ref []))

; events
(defmulti file-added class)
(defmulti file-removed class)

; create a proxy to interface with java
(def listener
     (proxy [JNotifyListener] []
       (fileCreated [wd rootPath name]
		    (println "got file created" (str rootPath "/" name))
		    (file-added (str rootPath "/" name)))
       (fileDeleted [wd rootPath name]
		    (println "got file delete" (str rootPath "/" name))
		    (file-removed (str rootPath "/" name)))
       (fileRenamed [wd rootPath from to])
     ))

;modified: (println (format "modified %s" (str rootPath "/" name))))

(defn watch-folder [folder]
  (common/ref-add watchIDs (. JNotify addWatch folder mask true listener)))

(defn watch-all-folders []
  (doseq [folder global/music-folders] 
    (watch-folder folder)))

(defn remove-all-watches []
  (doseq [id @watchIDs]
    (. JNotify removeWatch id))
  (common/ref-upd watchIDs []))

; start watching
(watch-all-folders)

;(def watchID (. JNotify addWatch global/watch-path mask true listener))
;(. JNotify removeWatch watchID)

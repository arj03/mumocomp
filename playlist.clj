(ns playlist)

; of type Track
(def tracks (ref []))

(defn add-track [t]
  (common/ref-add tracks t))

(defn add-collection [coll]
  (common/ref-upd-nil tracks (vec (flatten (conj @tracks coll)))))

(defn love-track [artist title]
  (let [track-filter #(and (= (:artist %) artist) (= (:title %) title))
	love-track #(if (track-filter %) (assoc % :loved true) %)]
    (common/ref-upd-nil tracks (map love-track @tracks))))

(defmulti all-tracks-removed class)

(defn clear []
  (all-tracks-removed nil)
  (common/ref-upd tracks []))

(defn remove-track [t]
  (let [mtracks (vec (filter #(not= t %) @tracks))]
    (cond (= mtracks nil)
       (clear)
      :else
       (common/ref-upd tracks mtracks)))
  nil)

(defmulti track-removed class)

(defn remove-track-i [index]
  (cond (= (count @tracks) 1)
	(clear)
	:else
	(do 
	  (common/ref-upd tracks (vec (concat (subvec @tracks 0 index) (subvec @tracks (inc index)))))
	  (track-removed index)))
  nil)

(add-watch tracks "save" (common/value-ref-wrapper-watch (common/save-to-db "playlist")))

; load
(common/ref-upd-nil tracks (common/load-from-db "playlist"))

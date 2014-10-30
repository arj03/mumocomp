(ns web)

(defn when-handle-command [cmd data]
  (println (str "handling command:" cmd))
  (when (= cmd "play")
    (playlist/clear))
  (println "adding collection")
  (playlist/add-collection data)
  (when (= cmd "play")
    (println "playing")
    (audio-player/play-track-i 0)))

(defn handle-audio-command [cmd params]
  (cond 
   (= cmd "reload") (audio/add-new-files)
   (= cmd "play-track") (audio-player/play-track-i (new Integer (:name params)))
   (and (= cmd "play") (= (:type params) "")) (audio-player/play)
   (= cmd "pause") (audio-player/pause)
   (= cmd "stop") (audio-player/stop)
   (= cmd "prev-track") (audio-player/prev-track)
   (= cmd "next-track") (audio-player/next-track)
   (= cmd "remove") (playlist/remove-track-i (new Integer (:name params)))
   (= cmd "clear-playlist") (playlist/clear)
   (= cmd "love-track") (audio-player/love-track)
   (= cmd "update-album") (audio/update-album-online-references (url-decode (:name params)))
   :else
   (let [decoded-name (url-decode (:name params))]
     (cond (= (:type params) "love")
           (let [only-loved #(filter :loved %)]
             (cond (= cmd "enqueue-genre")
                   (playlist/add-collection (only-loved (audio/tracks-by-genre decoded-name)))
                   (= cmd "enqueue-album")
                   (playlist/add-collection (only-loved (audio/tracks-by-album decoded-name)))
                   (= cmd "enqueue-artist")
                   (playlist/add-collection (only-loved (audio/tracks-by-artist decoded-name)))))
           (= (:type params) "genre")
           (when-handle-command cmd (audio/tracks-by-genre decoded-name))
           (= (:type params) "artist")
           (when-handle-command cmd (audio/tracks-by-artist decoded-name))
           (= (:type params) "album")
           (when-handle-command cmd (audio/tracks-by-album decoded-name))
           (= (:type params) "track")
           (when-handle-command cmd (@audio/tracks decoded-name))))
   :else "command not found")
  "")

(def last-artists-url (ref ""))

(defn set-last-artists-url [url]
  (common/ref-upd-nil last-artists-url url))

(defn mobile-audio-header-with-back [title]
  (html
   [:div {:data-role "header"}
    [:a {:href "" :data-rel "back" :data-icon "arrow-l"} "Back"]
    [:a {:href "/audio/artists" :data-ajax "false" :data-icon "arrow-u"} "Artists"]
    [:h1 title]]))

(defn audio-footer-options []
  [{:name "artists" :url @last-artists-url}
   {:name "playlist" :url "/audio/playlist" }
   {:name "playback" :url "/audio/playback"}
   {:name "movies" :url "/movie"}
   {:name "admin" :url "/admin"}
   ])

(defn mobile-audio-footer [option]
  (mobile-footer option (audio-footer-options)))

(defn add-dividers-and-format [list format-func]
  (let [letters (ref '())]
    (for [element list]
      (let [name (:name element)
	    letter (first name)]
	(cond (not (some #{letter} @letters))
	      (do 
		(common/ref-upd-nil letters (conj @letters letter))
		(str (html [:li {:data-role "list-divider"} letter]) (format-func element)))
	    :else
	    (format-func element))))))

(defn list-mobile-artists []
  (set-last-artists-url "/audio/artists")
  (html5
   [:head 
    [:title "Artists"]
    (mobile-header-js)
    (js "mobile-artists")
    ]
   [:body
    [:div {:data-role "page"}
     [:div {:data-role "header"}
      [:a {:href "" :id "reload" :class "ui-btn-right"} "Reload"]
      [:h1 "Artists"]]
     [:div {:data-role "content"}
      [:ul {:data-role "listview" :data-inset "true"}
       (let [format-artist #(html [:li (link-to {:data-ajax "false"} (str "/audio/artist/" (url-encode (:name %))) (:name %))])]
        (add-dividers-and-format (sort-by :name @audio/artists) format-artist))
       ]]
     (mobile-audio-footer "artists")
     ]]
   ))

(defn audio-cover-line [a]
  (html
   [:li
    [:a {:href (str "/audio/album/" (url-encode (:artist a)) "///" (url-encode (:name a)) "/") :class "go" :data-ajax "false" :name (url-encode (:name a))} [:img {:src (:large-cover a)}] (:name a)]
    [:a {:href "" :class "play" :data-theme "c" :name (url-encode (:name a))} "Play album"]]))

(defn mobile-artist [name]
  (set-last-artists-url (str "/audio/artist/" name))
  (let [artist (first (filter #(= (:name %) name) @audio/artists)), 
	title (str "Artist - " (:name artist))]
    (html5
     [:head 
      [:title title]
      (mobile-header-js)
      (js "mobile-albums")
      ]
     [:body 
      [:div {:data-role "page" :data-add-back-btn "true"}
       (mobile-audio-header-with-back title)
       [:div {:data-role "content"}
	[:ul {:data-role "listview" :data-inset "true"}
	 (map audio-cover-line (filter #(= (:artist %) (:name artist)) @audio/albums))
	 ]]
       (mobile-audio-footer "artists")]
      [:div {:data-role "dialog" :id "dialog"}
       [:div {:data-role "header"} [:h1 "Enqueue album?"]]
       [:div {:data-role "content"}
	 [:a {:href "" :id "enqueue-album-ok" :data-rel "back" :data-role "button"} "Enqueue"]
	 [:a {:href "" :id "enqueue-album-cancel" :data-rel "back" :data-role "button" :data-theme "c"} "Cancel"]
	 ]]
      ]
     )))

(defn mobile-artist-album [artist album]
  (set-last-artists-url (str "/audio/album/" artist "///" album "/"))
  (let [title (str "Album - " artist " - " album)]
    (html5
     [:head 
      [:title title]
      (mobile-header-js)
      (js "mobile-tracks")
      ]
     [:body 
      [:div {:data-role "page" :data-add-back-btn "true"}
       (mobile-audio-header-with-back title)
       [:div {:data-role "content"}
	[:ul {:data-role "listview" :data-inset "true"}
	 (let [filter-tracks #(and (= (:artist %) artist) (= (:album %) album))
	       format-track #(html [:li [:a {:href "" :onclick (str "enqueue_track('" (url-encode (:filename %)) "'); return false;") } (str (:artist %) " - " (:title %))]])]
	   (map format-track (filter #(filter-tracks %) (vals @audio/tracks))))
	 ]]
       (mobile-audio-footer "artists")]]
     )))

; TODO:
;
; select mode i playlist til delete

(defn list-mobile-playlist []
  (html5
   [:head 
    [:title "Playlist"]
    (mobile-header-js)
    (js "comet")
    (js "jquery.json-2.2.min")
    (js "mobile-playlist")
    ]
   [:body 
    [:div {:data-role "page"}
     (mobile-header "Playlist")
     [:div {:data-role "content"}
      [:ol {:data-role "listview"}
       (let [format-track #(html [:li [:a {:href "" :onclick (str "play_track('" %1 "'); return false;")} (str (:artist %2) " - " (:title %2))]])]
	 (map-indexed format-track @playlist/tracks))
       ]]
     (mobile-audio-footer "playlist")]]
   ))

(defn mobile-playback-status []
  (let [playing-track #(when (> (count @playlist/tracks) 0)
			 (nth @playlist/tracks (audio-player/playlist-index)))
	status #(let [track (playing-track)]
		 (cond (and (audio-player/is-playing) track)
		       (format "<b>%s:</b> %s / %s" 
			       (cond (audio-player/is-paused) "Paused"
				     :else "Playing")
			       (audio/format-length %)
			       (audio/format-length (:length track)))
		       :else "Not playing :("))
	art #(let [album (:album (playing-track))
		   artist (:artist (playing-track))]
	       (when (and album artist)
		 (audio/get-album-cover artist album)))
	track-info #(let [track (playing-track)]
		      (when track
			:track (audio/format-track track)))
	format-playback #(hash-map :index (audio-player/playlist-index)
				   :love (:loved (playing-track))
				   :art (art)
				   :paused (audio-player/is-paused)
				   :playing (audio-player/is-playing)
				   :track (track-info)
				   :status (status (% :player-position)))]
    (long-polling-scripts "mobile_playback" audio-player/state
			  format-playback)))

(defn mobile-playback-control []
  (html5
   [:head 
    [:title "Playback control"]
    (mobile-header-js)
    (js "jquery.json-2.2.min")
    (js "comet")
    (mobile-playback-status)
    (js "mobile-playback")
    (include-css "/css/mobile.css")
    ]
   [:body 
    [:div {:data-role "page"}
     (mobile-header "Playback control")
     [:div {:data-role "content"}
      [:div {:class "center"}
       [:img {:src "" :id "img"}]
       [:p {:id "playback-status" :class "center"}]
       [:p {:id "playback-info" :class "center"}]]
      [:center
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-prev"} "prev"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-toggle"} "pause"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-next"} "next"]
       [:br]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-stop"} "stop"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-love"} "love"]]
      ]
     (mobile-audio-footer "playback")]]
   ))

(defroutes audio-routes
  (GET "/" []
    (list-mobile-artists))
  (GET "/artists" []
    (list-mobile-artists))
  (GET ["/artist/:id" :id #".*"] [id]
    (mobile-artist (url-decode id)))
  (GET ["/album/:artist///:album/" :artist #".*" :album #".*"] [artist album]
    (mobile-artist-album (url-decode artist) (url-decode album)))
  (GET "/playlist" []
    (list-mobile-playlist))
  (GET "/playback" []
    (mobile-playback-control))
  (api (POST "/control" {params :params}
    (handle-audio-command (:command params) params))))

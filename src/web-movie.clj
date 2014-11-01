(ns web
  (:import (java.io File))
  (:import (java.io BufferedReader InputStreamReader))
  (:require [clojure.string :as string])
  )

(def movie-footer-options
  [{:name "movies" :url "/movie/"}
   {:name "reload folders" :url "/movie/reload"}
   {:name "controls" :url "/movie/controls"}
   {:name "audio" :url "/audio"}
   {:name "admin" :url "/admin"}
   ])

(defn mobile-movie-footer [option]
  (mobile-footer option movie-footer-options))

(defn path-ok [path]
  (some #(. path startsWith %) global/movie-folders))

(defn get-files [path]
  (cond (= path "")
	(flatten (map #(movie/list-folder %) global/movie-folders))
	:else
	(cond (path-ok path)
	      (movie/list-folder path)
	      :else '())))

(defn mobile-movie-header-with-back [title]
  (html
   [:div {:data-role "header"}
    [:a {:href "" :data-rel "back" :data-icon "arrow-l"} "Back"]
    [:h1 title]]))

(defn format-movie-line [movie]
  (html
   (cond (:modified movie)
         [:li (link-to {:data-ajax "false" :class "file"} (str "/movie/" (url-encode (:path movie))) (:name movie))]
         :else
         [:li (link-to {:data-ajax "false" :class "go" :name (url-encode (:path movie))} (str "/movie/" (url-encode (:path movie))) (:name movie))])))

(defn internet-movie-line-helper [m movie info]
  (let [imdb-info #(html [:div {:style "padding-right: 10px;"} (str "imdb: " (:imdb-rating %))])
	rotten-info #(html [:div {:style "padding-right: 10px;"} (str "rotten tomatoes: " (:rotten-tomatoes-rating %))])]
  (html
   [:li
    (link-to (conj {:data-ajax "false"} m) (str "/movie/" (url-encode (:path movie)))
             (when (:image info)
               [:img {:width 110 :src (:image info)}])
             (:name movie)
             [:br]
             (when (:imdb-rating info)
               (imdb-info info))
             (when (:rotten-tomatoes-rating info)
               (rotten-info info))
             (when (:year info)
               (str "Year: " (:year info)))
             )])))

(defn internet-movie-line [movie]
  (let [info (@movie/movie-infos (:path movie))]
    (cond (:modified movie)
          (internet-movie-line-helper {:class "file"} movie info)
          :else
          (internet-movie-line-helper {:class "go" :name (url-encode (:path movie))} movie info))))

(defn has-info [path]
  (cond (= path "")
        (some #(movie/is-internet-info-folder %) global/movie-folders)
        :else
        (movie/is-internet-info-folder path)))

(defn movies [path]
  (let [files (sort (common/simple-cmp :name) (get-files path))]
    (html5
     [:head 
      [:title "Movies"]
      (mobile-header-js)
      (js "movie")
      ]
     [:body
      [:div {:data-role "page"}
       (mobile-movie-header-with-back "Movies")
       [:div {:data-role "content"}
        [:ul {:data-role "listview" :data-inset "true"}
         (cond (has-info path)
               (map internet-movie-line files)
               :else
               (map format-movie-line files))
         ]]
       (mobile-movie-footer "movies")
       ]
      [:div {:data-role "dialog" :id "dialog"}
       [:div {:data-role "header"} [:h1 "Update movie info?"]]
       [:div {:data-role "content"}
        [:a {:href "" :id "update-internet-info-ok" :data-rel "back" :data-role "button"} "Update"]
        [:a {:href "" :id "update-internet-info-cancel" :data-rel "back" :data-role "button" :data-theme "c"} "Cancel"]
        ]]
      ]
     )))

(defn play-movie [path]
  (when (not= path "")
  (println (str "playing:" (str (global/playback-commands (movie/extension path)) " '" path "'")))
  (movie/update-last-watched path)
  (common/run-simple-command-with-output common/cmdout (vector "/bin/bash" "-c" (str (global/playback-commands (movie/extension path)) " '" path "'")))))

(use 'clojure.java.io)

; control mplayer
(defn send-command [pid cmd]
  (when pid
    (with-open [wrtr (writer (str "/proc/" pid "/fd/0"))]
      (.write wrtr cmd))))

(defn get-pid []
  (let [o (.. Runtime getRuntime (exec "ps -C mplayer -o pid="))
        r (-> o .getInputStream InputStreamReader. BufferedReader.)
        f (first (line-seq r))]
    (when f
      (string/trim f))))

;(print "sending command")
; m (forward), n (REWIND) - defined in .mplayer/input.conf

(def last-command (atom { :cmd "" :lastrun (. System currentTimeMillis) :multiplier 1 }))

(defn handle-movie-command [cmd params]
  (let [last @last-command
        pid (get-pid)
        same-as-last (fn [] (and 
                             (= (:cmd last) cmd) 
                             (< (- (. System currentTimeMillis) (:lastrun last)) 10000)))]
    (swap! last-command assoc 
           :cmd cmd 
           :lastrun (. System currentTimeMillis)
           :multiplier (min 5 (cond (same-as-last) (inc (:multiplier last)) :else 1)))
    (cond (= cmd "play-movie")
          (play-movie (url-decode (:name params)))
          ; control mplayer
          (= cmd "reverse")
          (dotimes [n (:multiplier @last-command)]
            (send-command pid "n"))
          (= cmd "forward")
          (dotimes [n (:multiplier @last-command)]
            (send-command pid "m"))
          (= cmd "pause")
          (send-command pid "p")
          (= cmd "stop")
          (send-command pid "q")
          (= cmd "osd")
          (send-command pid "o")
          (= cmd "subtitle")
          (send-command pid "j")
          (= cmd "reload")
          (movie/read-files)
          (= cmd "update-internet-info")
          (movie/update-movie (url-decode (:name params)))
          )
    ""))

(defn mobile-movie-control []
  (html5
   [:head 
    [:title "Playback control"]
    (mobile-header-js)
    (js "mobile-movie-playback")
    (include-css "/css/mobile.css")
    ]
   [:body 
    [:div {:data-role "page"}
     (mobile-header "Playback control")
     [:div {:data-role "content"}
      [:center
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-reverse"} "reverse"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-toggle"} "pause"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-forward"} "forward"]
       [:br]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-stop"} "stop"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-osd"} "OSD"]
       [:a {:href "" :data-role "button" :data-inline "true" :id "playback-subtitle"} "subtitle"]]
      ]
     (mobile-movie-footer "controls")]]
   ))

(defroutes movie-routes
  (api (POST "/control" {params :params}
             (handle-movie-command (:command params) params)))
  (GET "/controls" []
    (mobile-movie-control))
  (GET ["/:path" :path #".*"] [path]
    (movies (url-decode path)))
  (GET "/" []
    (movies "")))

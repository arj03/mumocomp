(ns metal-archives
  (:use net.cgrand.enlive-html)
  (:require [clojure.string :as string])
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
)

;(defn get-url [artist album]
;  (str "http://www.google.com/search?q=site%3Ametal-archives.com/release.php+%22" (screen-scraping/url-encode artist) "%22+%22" (screen-scraping/url-encode album) "%22"))

(defn get-google-json-url [artist album]
  (str "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=site:metal-archives.com/release.php+%22" (screen-scraping/url-encode artist) "%22+%22" (screen-scraping/url-encode album) "%22"))

(defn fetch-json [url]
  (json/read-json (:body (client/get url {:headers (hash-map "user-agent" "Clojure-HttpClient/0.1")}))))

(defn get-url [artist album]
  (when (and artist album)
    (:unescapedUrl (first (:results (:responseData (fetch-json (get-google-json-url artist album))))))))

(defn get-info [artist album]
  (let [url (get-url artist album)]
    (when url
      (let [dds (select (screen-scraping/fetch-url url) {[:dt] [:dd]})
	    year-text (first (:content (nth (first (filter #(= (first (:content (first %))) "Release date:") dds)) 2)))
	    year (.substring year-text (- (count year-text) 4))
	    rating-text (string/trim (last (:content (nth (first (filter #(= (first (:content (first %))) "Reviews:") dds)) 2))))]
	(when (not= year-text "")
	  (hash-map :url url :year (new Integer year) :rating rating-text))))))

(defn get-info-from-url [url]
  (when url
    (let [fetched-url (screen-scraping/fetch-url url)
	  bds (select fetched-url [:td :b])
	  year-text (text (second bds))
	  rating-text (if (> (count bds) 2) (text (nth bds 2)) "")]
      (when (not= year-text "")
	(cond (= rating-text "Total playing time")
	      (hash-map :url url :year (new Integer year-text) :rating "")
	      :else
	      (hash-map :url url :year (new Integer year-text) :rating rating-text))))))

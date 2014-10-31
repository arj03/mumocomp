(ns rotten
  (:use net.cgrand.enlive-html)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
)

(defn get-google-url [name]
  (str "http://www.google.com/search?q=site%3Arottentomatoes.com+%22" (screen-scraping/url-encode name) "%22"))

;(defn get-google-url [name]
;  (str "http://www.google.com/#sclient=psy&hl=en&q=site:rottentomatoes.com+" (screen-scraping/url-encode name)))

(defn get-google-json-url [name]
  (str "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=site:rottentomatoes.com+%22" (screen-scraping/url-encode name) "%22"))

(defn fetch-json [url]
  (json/read-json (:body (client/get url {:headers (hash-map "user-agent" "Clojure-HttpClient/0.1")}))))

;(defn get-url [name]
;  (:unescapedUrl (first (:results (:responseData (fetch-json (get-google-json-url name)))))))

(defn get-url [name]
  (let [ghtml (screen-scraping/fetch-url (get-google-url name))
        element (:content (first (select ghtml [:div :cite])))]
    (str (first element) (first (:content (second element))))))

(defn get-rating [name]
  (let [url (get-url name)]
    (when (and url (not= url ""))
      (let [data (screen-scraping/fetch-url (str "http://" url))
            rating (first (:content (first (select data [:span#all-critics-meter]))))
            img (:src (:attrs (first (select data [:div.movie_poster_area :img]))))]
        (hash-map :url (str "http://" url) :rating rating :img img)))))

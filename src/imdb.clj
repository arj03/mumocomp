(ns imdb
  (:use net.cgrand.enlive-html)
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
)

(defn get-google-json-url [name]
  (str "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=site:imdb.com+%22" (screen-scraping/url-encode name) "%22"))

(defn fetch-json [url]
  (json/read-json (:body (client/get url {:headers (hash-map "user-agent" "Clojure-HttpClient/0.1")}))))

(defn get-url [name]
  (:unescapedUrl (first (:results (:responseData (fetch-json (get-google-json-url name)))))))

(defn get-img-url [data]
  (:src (:attrs (first (select (:content (first (select data [:td#img_primary]))) [:img])))))

(defn get-rating [name]
  (let [url (get-url name)]
    (when url
      (let [data (screen-scraping/fetch-url url)
            rating (first (:content (first (select data [:strong :span]))))
            year (first (:content (first (select data [:h1 :span :a]))))]
        (hash-map :url url :year (Integer. year) :rating rating :img (get-img-url data))))))

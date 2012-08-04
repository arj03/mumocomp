(ns wikipedia
  (:use net.cgrand.enlive-html)
)

(defn get-published-date [url]
  (first (map text (select (screen-scraping/fetch-url url) [:td.published]))))

(defn extract-year [published]
  (cond published
	(new Integer (re-find #"\d\d\d\d" published))
	:else 0))

(defn get-url [artist album]
  (cond (and artist album)
    (str "http://www.google.com/search?q=site%3Aen.wikipedia.org/wiki/+%22" (screen-scraping/url-encode artist) "%22+%22" (screen-scraping/url-encode album) "%22+" "album")
    :else ""))

(defn get-published-year [artist album]
  (when (and artist album)
    (let [url (screen-scraping/get-first-google-result get-url artist album)]
      (when url
        (hash-map :url url :year (extract-year (get-published-date url)))))))

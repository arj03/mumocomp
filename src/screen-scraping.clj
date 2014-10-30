(ns screen-scraping
  (:use net.cgrand.enlive-html)
  (:import java.net.URLEncoder)
  (:require [clojure.string :as string])
  (:require [clj-http.client :as client])
)

(defn url-encode [param]
  (URLEncoder/encode param "utf-8"))

(defn fetch-url [url]
  (when (and url (not= (. url trim) ""))
    (html-resource (java.io.StringReader. (:body (client/get url {:headers (hash-map "user-agent" "Clojure-HttpClient/0.1")}))))))

(defn get-first-google-result [get-url artist album]
  (:href (:attrs (first (select (fetch-url (get-url artist album)) [:a.l])))))

(def patterns (vector [" (Reissue)" ""] [" (disk 1)" ""] [" (Digipack)" ""]))

(defn safe-name-helper [param vec]
  (string/replace param (first vec) (second vec)))

(defn get-safe-name [param]
  (reduce safe-name-helper param patterns))
  

(ns web
  (:require [clojure.data.json :as json])
  (:import java.net.URLDecoder java.net.URLEncoder)
  (:use compojure.core, compojure.handler, ring.adapter.jetty)
  (:use hiccup.core, hiccup.page, hiccup.element)
  (:require [compojure.route :as route])
)

(import '(java.util.concurrent BlockingQueue LinkedBlockingQueue TimeUnit))

(defn url-decode [param]
  (URLDecoder/decode param "iso-8859-1"))

(defn url-encode [param]
  (URLEncoder/encode param "iso-8859-1"))

(defn js [script]
  (include-js (str "/js/" script ".js")))

(defn init-json-data [dataref filter-func name]
  (str
   (cond filter-func
      (str "var " name " = " (json/json-str (filter-func @dataref)) ";")
     :else
      (str "var " name " = " (json/json-str @dataref) ";"))
   (str "var " name "id = " (long-polling/register-polling dataref filter-func) ";")))

(defn comet-load [name id-name]
  (str "$(document).ready( function() { $.comet(function(data) { " name " = $.evalJSON(data); update_" name "(); }, " id-name "); });"))

(defn long-polling-scripts [name ref filter-func]
  (let [register (init-json-data ref filter-func name)
        comet (comet-load name (str name "id"))]
    (html [:script {:type "text/javascript"} register]
	  (js (str "list-" name))
	  [:script {:type "text/javascript"} comet])))

(defn activity [id]
  (long-polling/get-msg id))

(defn mobile-header-js []
  (html 
   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
   (include-css "/css/jquery.mobile-1.4.5.min.css")
   (js "jquery-2.1.1.min")
   (js "jquery.mobile-1.4.5.min")
   (js "notification")
   ))

(defn mobile-header [title]
  (html
   [:div {:data-role "header"}
    [:h1 title]]))

(defn mobile-footer [option options]
  (html
   [:div {:data-role "footer" :data-position "fixed" :data-theme "b"}
    [:div {:data-role "navbar"}
     [:ul
      (map #(html [:li [:a {:href (:url %) :class (cond (= (:name %) option) "ui-btn-active" :else "") :rel "external"} (:name %)]])
           options)
      ]]]))

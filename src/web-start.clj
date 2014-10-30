(ns web
  (:require [ring.util.response :as resp]))

(defn load-static-file []
  (route/files "/" {:root global/web-folder}))

(defroutes my-routes
  (context "/audio" [] audio-routes)
  (context "/movie" [] movie-routes)
  (context "/admin" [] admin-routes)
  (GET ["/activity/:id" :id #".*"] [id]
    (activity (new Integer id)))
  (GET ["/template/:filename" :filename #".*"] [filename]
    (load-static-file))
  (GET ["/js/:filename" :filename #".*"] [filename]
    (load-static-file))
  (GET ["/css/:filename" :filename #".*"] [filename]
    (load-static-file))
  (GET ["/img/:filename" :filename #".*"] [filename]
    (load-static-file))
  (GET ["/images/:filename" :filename #".*"] [filename]
    (load-static-file))
  (GET "/" []
    (resp/redirect "/audio"))
  (route/not-found (html [:h1 "Page not found!"])))

;(run-jetty (var my-routes) {:port 6666})
(run-jetty (var my-routes) {:port 5000})

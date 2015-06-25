(ns web)

(def admin-footer-options
  [{:name "admin" :url "/admin/"}
   ;{:name "audio" :url "/audio/"}
   {:name "movies" :url "/movie/"}])

(defn admin [path]
  (html5
   [:head 
    [:title "Administration"]
    (mobile-header-js)
    ]
   [:body
    [:div {:data-role "page"}
     (mobile-header "Administration")
     [:div {:data-role "content"}
      [:ul {:data-role "listview" :data-inset "true"}
       [:li (link-to "/admin/reboot" "Reboot")]
       [:li (link-to "/admin/shutdown" "Shutdown")]
       ]]
     (mobile-footer "admin" admin-footer-options)
     ]
    ]
   ))

(defroutes admin-routes
  (GET "/shutdown" []
	(common/cmd (into-array (vector "sudo" "/sbin/shutdown" "-h" "now")))
       (str "<html>Shutting down</html>"))
  (GET "/reboot" []
 	(common/cmd (into-array (vector "sudo" "/sbin/reboot")))
       (str "<html>Rebooting</html>"))
  (GET "/" []
    (admin "")))

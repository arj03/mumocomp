(ns long-polling
  (:require [clojure.data.json :as json]))

(def timeout 100) ; sec

;[clojure.contrib.http.agent :as h]
;(use 'clojure.contrib.http.agent)
; fetch, this will wait if no messages are active
;(string (http-agent "http://127.0.0.1/activity?id=1"))

(import '(java.util.concurrent BlockingQueue LinkedBlockingQueue TimeUnit))

(defstruct Poller
  :id
  :watching
  :queue
  :filter-func
  :last-polled
)

(let [i (atom 0)]
  (defn generate-unique-id
    "Returns a distinct numeric ID for each call."
    []
    (swap! i inc)))

; id => ref it's watching
(def pollers (ref []))

; gc
(defn has-expired [poller]
  (> (- (System/currentTimeMillis) (:last-polled poller)) (* 2 timeout 1000)))

(defn gc-pollers []
  (common/ref-upd pollers (doall (filter #(not (has-expired %)) @pollers))))

(def pollers-gc (agent nil))

(defn loop-gc-pollers [state]
  (gc-pollers)
  (Thread/sleep (* timeout 1000))
  (send pollers-gc loop-gc-pollers))

(defn start-gc-pollers []
  (send pollers-gc loop-gc-pollers))

(start-gc-pollers)

(defn add-to-pollers [watching filter-func]
  (let [p (struct Poller (generate-unique-id) watching (new LinkedBlockingQueue) filter-func (System/currentTimeMillis))]
    (common/ref-add pollers p)
    p))

; Because compojure doesn't allow us to know when a client has
; disconnect, we send out messages every timeout s. This is also to avoid
; memory leaks, so we consider this a not-so-long-polling-implementation

(defn get-msg [id]
  (let [q (common/find-first #(= (:id %) id) @pollers)]
    (cond q
      (do 
	(let [updated (merge q { :last-polled (System/currentTimeMillis) })
	      rest (filter #(not= (:id %) id) @pollers)]
	  (common/ref-upd pollers (doall (cons updated rest)))
	  (let [val (. (:queue q) poll timeout TimeUnit/SECONDS)]
	    (cond val val
		  :else
		  (json/json-str {:msg "timeout"})))))
      :else
       (format "id not found %s" id))))

(defn add-msg [id msg old]
  (when (not= msg old)
    (let [qe (common/find-first #(= (:id %) id) @pollers)]
      (when qe
	(cond (:filter-func qe)
	      (. (:queue qe) put (json/json-str ((:filter-func qe) msg)))
	      :else
	      (. (:queue qe) put (json/json-str msg)))))))

;(defn write-json [id data]
;  (h/http-agent (str "http://127.0.0.1:9999/publish?id=" id) 
;		:method "POST" :body (json/json-str{ :data data }) 
;		:handler (fn [agnt] (println (h/headers agnt)))))

; returns the unique id that will be publishing using
(defn register-polling [ref-to-watch filter-func]
  (let [p-id (:id (add-to-pollers ref-to-watch filter-func))]
    (add-watch ref-to-watch "polling" (fn [_ ref old new] 
					(add-msg p-id new old)))
    p-id))

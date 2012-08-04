(ns my-app
  (:require [swank.swank]))
(swank.swank/start-server :host "192.168.0.7" :port 4005 :dont-close true)

;(use '[swank.swank])
;(swank.swank/ignore-protocol-version "2009-03-09")
;(start-server ".slime-socket" :port 4005 :dont-close true)
; :encoding "utf-8"

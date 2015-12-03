(ns dcm2mongo.core
  (:gen-class)
  (:use dcm2mongo.mongo-service
        dcm2mongo.watch-dir
        )

  (:require [clojure.tools.logging :as log]))

(defn -main [& args]
  (mk-connect)
  (set-index )
  ;(watch-start "D:\\internal_storage\\caps\\images" true)
  (watch-start "D:\\internal_storage\\caps\\images" true)
  )


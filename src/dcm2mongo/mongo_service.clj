(ns dcm2mongo.mongo-service
  (:require [clojure.tools.logging :as log]
            [somnium.congomongo :as mn]
            )
  (:use [somnium.congomongo.config])
  (:gen-class)
  (:import (java.util Date)))

(def dcmdb (atom nil))

(defn mk-connect []
  (let [conn (mn/make-connection :test_dcm :host "127.0.0.1" :port 27017)]
    (log/debug conn)
    (reset! dcmdb conn)))

(defn- find-by-SOPClassUID [id]
  (mn/with-mongo @dcmdb
                 (first (mn/fetch :dcm1 :where {:00080018.Value [id]} :sort {:v -1} :limit 1))))


(defn json2mongo [obj]
  (log/debug "json2mongo")
  (let [sopcuid (first (get-in obj [:00080018 :Value]))
        _ (log/debug sopcuid)
        existObj (find-by-SOPClassUID sopcuid)]
    (mn/with-mongo @dcmdb
                   (mn/insert! :dcm1 (assoc obj :created (Date.) :v (inc (or (:v existObj) 0)))))))

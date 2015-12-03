(ns dcm2mongo.mongo-service
  (:require [clojure.tools.logging :as log]
            [somnium.congomongo :as mn])
  (:use [somnium.congomongo.config]
        [clojure.data :as data])
  (:import (java.util Date)
           (java.io FileInputStream))
  (:gen-class)
  )

(def dcmdb (atom nil))

(defn mk-connect []
  (let [conn (mn/make-connection :test_dcm :host "127.0.0.1" :port 27017)]
    (log/debug conn)
    (reset! dcmdb conn)))

(defn set-index []
  (mn/with-mongo @dcmdb (mn/add-index! :dcm1 [:00080018.Value]))
  )

(defn- find-by-SOPClassUID [id]
  (mn/with-mongo @dcmdb
                 (first (mn/fetch :dcm1 :where {:00080018.Value [id]} :sort {:v -1} :limit 1))))


(defn json2mongo [obj ^String path]
  (let [sopcuid (first (get-in obj [:00080018 :Value]))
        _ (log/info sopcuid)
        existObj (find-by-SOPClassUID sopcuid)]
    (let [[a b c] (data/diff obj (dissoc existObj :created :v :_id))]
      (when (or a b)
        (try
          (mn/with-mongo @dcmdb
                         (let [obj (mn/insert! :dcm1 (assoc obj :created (Date.) :v (inc (or (:v existObj) 0))))]
                           (with-open [stream (FileInputStream. path)]
                             (mn/insert-file! :dcm1fs stream :filename (str (:_id obj)) :contentType "application/dicom"))))
          (catch Exception e (log/error "File=" path ";" (.getMessage e)) nil)
          )))))

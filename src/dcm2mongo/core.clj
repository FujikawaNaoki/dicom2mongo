(ns dcm2mongo.core
  (:gen-class)
  (:use dcm2mongo.dcm-parser
        dcm2mongo.mongo-service
        )

  )

(defn -main [& args]
  (when-let [obj (dcm2parse (clojure.java.io/file "D:\\0009g6wx.dcm"))]
    (json2mongo obj)
    )
  )
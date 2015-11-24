(ns dcm2mongo.dcm-parser
  (:import (java.io File ByteArrayOutputStream FilterOutputStream OutputStream)
           (org.dcm4che3.io DicomInputStream DicomStreamException DicomInputStream$IncludeBulkData)
           (org.dcm4che3.json JSONWriter)
           (javax.json Json))
  (:use [dcm2mongo.personal-anonymizer])
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json :only [parse-string]])
  (:gen-class)
  )


(defn- json-generator [^OutputStream out]
  (.createGenerator
    (Json/createGeneratorFactory (hash-map )) out))

(defn- setDicomIsConfig [^DicomInputStream in]
  (doto in
    (.setBulkDataDirectory nil)
    (.setBulkDataFilePrefix "blk")
    (.setBulkDataFileSuffix nil)
    (.setConcatenateBulkDataFiles false)
    (.setIncludeBulkData DicomInputStream$IncludeBulkData/URI)))

(defn- dcm-parse [^DicomInputStream in out]
  (let [json-gen (json-generator out)]
    (doto (setDicomIsConfig in)
      (.setDicomInputHandler (JSONWriter. json-gen))
      (.readDataset -1 -1))
    (.flush json-gen)))

(defn- read-file [^File f ^String encode]
  (with-open [byte-array (ByteArrayOutputStream.)
               output (FilterOutputStream. byte-array)]
    (try
      (dcm-parse (DicomInputStream. f) output)
      (json/parse-string (.toString byte-array encode) true)
      (catch DicomStreamException e (log/info e) nil)))
  )

(defn dcm2parse [^File f]
  (when-let [obj (read-file f "UTF-8")]
    (let [jsonObj (attrbute-anonymization obj)]
      jsonObj
      )
    )
  )

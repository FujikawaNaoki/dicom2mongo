(defproject dcm2mongo "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Dfile.encoding=UTF-8"]
  :dependencies [
                 ;clojure
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;logging
                 [ch.qos.logback/logback-classic "1.1.3"]
                 ;json
                 [cheshire "5.5.0"]
                 ;dicom lib
                 [org.dcm4che/dcm4che-core "3.3.7"]
                 [org.dcm4che/dcm4che-json "3.3.7"]
                 ;mongodb
                 [congomongo "0.4.6"]
                ]
  :plugins    [[lein-ancient "0.6.8"]]
  :repositories [["www.dcm4che.org" "http://www.dcm4che.org/maven2"]]
  ;:aot dcm2mongo.core
  :main dcm2mongo.core
  )

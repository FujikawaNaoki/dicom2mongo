(ns dcm2mongo.watch-dir
  (:import (java.nio.file FileSystems FileSystem Path Paths WatchEvent$Kind
                          StandardWatchEventKinds Files SimpleFileVisitor
                          FileVisitResult WatchService LinkOption WatchKey WatchEvent)
           (java.nio.file.attribute BasicFileAttributes)
           (java.io File FileFilter))
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async :refer [>! <! put! take! timeout chan go]])
  (:use [dcm2mongo.dcm-parser]
        [dcm2mongo.mongo-service]
        [clojure.java.io :as io]
        )
  (:gen-class)
  )
(def ENTRY_CREATE StandardWatchEventKinds/ENTRY_CREATE)
(def ENTRY_DELETE StandardWatchEventKinds/ENTRY_DELETE)
(def ENTRY_MODIFY StandardWatchEventKinds/ENTRY_MODIFY)
(def ENTRY_OVERFLOW StandardWatchEventKinds/OVERFLOW)
(def NOFOLLOW_LINKS LinkOption/NOFOLLOW_LINKS)

(def watcher (atom nil))
(def watch_keys (atom nil))
(def watch_recusive (atom false))
(def init-touch (atom false))
(def check-files (chan))

(defn- ^Path getNioPath [^String s]
  (Paths/get s (into-array String [])))

(defn- register
  ([^Path sPath]
   (let [k (.register sPath @watcher
                      (into-array WatchEvent$Kind (list ENTRY_CREATE ENTRY_DELETE ENTRY_MODIFY)))]
     (when (and (nil? (get @watch_keys k)) @init-touch)
       (when-let [col (not-empty
                        (map #(.getPath %)
                             (.listFiles
                               (.toFile sPath)
                               (proxy [FileFilter] []
                                 (accept [^File lf]
                                   (and (.isFile lf) (.endsWith (.getName lf) ".dcm")))))))]
         (put! check-files col)))
     (swap! watch_keys assoc k (str sPath))))
  ([^Path sPath recursive]
   (if recursive
     (Files/walkFileTree
       sPath
       (proxy [SimpleFileVisitor] []
         (preVisitDirectory [^Path dir ^BasicFileAttributes attrs]
           (register dir)
           FileVisitResult/CONTINUE)))
     (register sPath)
     ))
  )

(defn- concats
  "A non variadic version of concat"
  [s]
  (lazy-seq (concat (first s) (concats (rest s)))))

(defmulti doEvent (fn [event] (:kind event)))

(defmethod doEvent ENTRY_OVERFLOW [event]
  ;nothing do prcess)
  )
(defmethod doEvent ENTRY_CREATE [event]
  (if (Files/isDirectory (:target event) (into-array LinkOption [NOFOLLOW_LINKS]))
    (register (:target event) @watch_recusive)
    (log/info "#####ENTRY_CREATE;" event " -> nothing todo;")))


(defmethod doEvent ENTRY_MODIFY [event]                     ;directory change nothing to do;
  (when-not (Files/isDirectory (:target event) (into-array LinkOption [NOFOLLOW_LINKS]))
    (log/info "#####ENTRY_MODIFY;" event)
    (let [^File f (.toFile (:target event))]
      (when (and (.exists f) (.isFile f) (.endsWith (.getName f) ".dcm"))
        (log/debug "into-check-files:" (.getPath f))
        (put! check-files (.getPath f))))))

(defmethod doEvent ENTRY_DELETE [event]
  (log/info "ENTRY_DELETE;" (:target event))
  (let [deletePath (str (:target event))]
    (doseq [k (filter #(= deletePath (get @watch_keys %)) (keys @watch_keys))]
      (swap! watch_keys dissoc k))))

(defn- watch-loop
  "各種イベント後の処理"
  [^WatchService service]
  (loop []
    (let [^WatchKey key (.take service)]
      (doseq [^WatchEvent event (.pollEvents key)]
        (when-not (= (.name (.kind event)) (.name ENTRY_OVERFLOW))
          (when-let [^String dirStr (get @watch_keys key)]
            (let [^Path dir (getNioPath dirStr)]
              (doEvent (assoc {} :kind (.kind event)
                                 :name (.context event)
                                 :dir dir
                                 :target (.resolve dir (.context event))
                                 :key key))))))
      (if (.reset key)
        (recur)
        (do
          (swap! watch_keys dissoc key)
          (when (not (empty? @watch_keys))
            (recur)))
        )
      )
    )
  )
(defn- recur-exist-file-touch [^String path]
  (prn "## " (.isDirectory (io/file path)))
  (when (.isDirectory (io/file path))
    (map (fn [f]
           (log/debug "touch-file;" f)
           (.setLastModified f (System/currentTimeMillis)))
         (file-seq (io/file path)))))

(defn- file2mongo []
  (go
    (while true
      (let [paths (<! check-files)]
        ;(prn "##async## " paths)
        (doall (pmap
                 (fn [path]
                   (when-let [obj (dcm2parse (io/file path))]
                     (json2mongo obj path))) (if (coll? paths) paths (list paths))))
        ))))

(defn watch-start
  ([^String startDir ^Boolean initTouch]
   (reset! init-touch (boolean initTouch))
   (reset! watcher (.newWatchService ^FileSystem (FileSystems/getDefault)))
   (reset! watch_keys {})
   (reset! watch_recusive true)
   (file2mongo)
   (register (getNioPath startDir) @watch_recusive)
   (watch-loop @watcher))
  ([^String startDir] (watch-start startDir false))
  )

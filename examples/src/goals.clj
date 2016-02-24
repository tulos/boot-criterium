(ns goals)

(def millis nil)

(defn sleep-2x []
  (Thread/sleep (* 2 (or millis 1000))))

(defn sleep-1x []
  (Thread/sleep (or millis 1000)))

(defn set-up []
  (println "Setting up:" millis)
  (alter-var-root #'millis (constantly 500)))

(defn tear-down []
  (println "Tearing down:" millis)
  (alter-var-root #'millis (constantly nil)))

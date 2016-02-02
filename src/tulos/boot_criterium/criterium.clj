(ns tulos.boot-criterium.criterium
  (:require [clojure.walk :as walk]
            [clojure.string :as string]
            [criterium.core :as crit]
            [boot.util :as bu]
            [tulos.boot-criterium.util :as u]))

(defn- resolve-code [code]
  (if (symbol? code)
    (u/resolve-var code)
    (eval `(fn [] ~code))))

(defn- remove-criterium-types [result]
  (walk/postwalk
    (fn [v]
      (if (instance? criterium.core.OutlierCount v)
        (into {} v)
        v))
    result))

(defn- with-scale [[e]]
  [e (crit/scale-time e)])

(defn- with-sqrt-scale [[e]]
  [(Math/sqrt e) (crit/scale-time (Math/sqrt e))])

(defn- interpret-measurements
  [{:keys [execution-count sample-count
           mean sample-mean variance sample-variance
           lower-q upper-q tail-quantile overhead
           outlier-variance]
    :as result}]
  (assoc result :interpreted
    {:sample-mean (with-scale sample-mean)
     :mean (with-scale mean)
     :sample-variance (with-sqrt-scale sample-variance)
     :variance (with-sqrt-scale variance)
     :lower-q (with-scale lower-q)
     :upper-q (with-scale upper-q)
     :lower-quantile [tail-quantile [100 "%"]]
     :upper-quantile [(- 1 tail-quantile) [100 "%"]]
     :overhead (with-scale [overhead])
     :outlier-effect (crit/outlier-effect outlier-variance)
     :evaluation-count (* execution-count sample-count)}))

(defmacro binding-var [[v new-v] & body]
  `(let [orig# ~v]
     (try
       (alter-var-root (var ~v) (constantly ~new-v))
       ~@body
       (finally
         (alter-var-root (var ~v) (constantly orig#))))))

(defn- with-ln [f]
  (fn [& args]
    (let [s (-> (string/join " " args)
                (.replaceAll "%" "%%")
                (string/trim))]
      (f s))
    (f "\n")))

(defn- with-progress-at-boot-info [f]
  (binding-var [crit/progress (with-ln bu/info)]
    (binding [crit/*report-progress* true]
      (f))))

(defn- with-debug-at-boot-dbug [f]
  (binding-var [crit/debug (with-ln bu/dbug)]
    (binding [crit/*report-debug* true]
      (f))))

(defn- println-warnings [& args]
  (let [s (string/join " " args)]
    (if (.startsWith s "WARNING:")
      ((with-ln bu/warn) (.substring s 8))
      (println s))))

(defn- println-no-warnings [& args]
  (let [s (string/join " " args)]
    (when-not (.startsWith s "WARNING:")
      (println s))))

(defn- with-warn-at-boot-warn [f]
  (binding-var [crit/warn (with-ln bu/warn)]
    (binding-var [clojure.core/println println-warnings]
      (binding [crit/*report-warn* true]
        (f)))))

(defn- with-suppress-warn [f]
  (binding-var [clojure.core/println println-no-warnings]
    (f)))

(defn run-goal [code params {:keys [quick? progress? debug? warn?]}]
  (let [f (resolve-code code)
        f (if quick?
            #(crit/quick-benchmark* f params)
            #(crit/benchmark* f params))
        f (if progress?
            #(with-progress-at-boot-info f)
            f)
        f (if debug?
            #(with-debug-at-boot-dbug f)
            f)
        f (if warn?
            #(with-warn-at-boot-warn f)
            #(with-suppress-warn f))]
    (bu/dbug "Running a %s benchmark for %s..."
             (when quick? "quick" "full") code)
    (-> (f)
        (remove-criterium-types)
        (interpret-measurements))))

(ns tulos.boot-criterium.criterium
  (:require [clojure.walk :as walk]
            [clojure.string :as string]
            [criterium.core :as crit]
            [boot.util :as bu]
            [tulos.boot-criterium.util]))

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

(defn- with-ln [f]
  (fn [& args]
    (let [s (-> (string/join " " args)
                (.replaceAll "%" "%%")
                (string/trim))]
      (f s))
    (f "\n")))

(defn- with-progress-at-boot-info [f]
  (with-redefs [crit/progress (with-ln bu/info)]
    (binding [crit/*report-progress* true]
      (f))))

(defn- with-debug-at-boot-dbug [f]
  (with-redefs [crit/debug (with-ln bu/dbug)]
    (binding [crit/*report-debug* true]
      (f))))

(defn- with-warn-at-boot-warn [f]
  (with-redefs [crit/warn (with-ln bu/warn)]
    (binding [crit/*report-warn* true]
      (f))))

(defn run-goal [{:keys [before after goal]} params {:keys [quick? progress? debug? warn?]}]
  (let [f (if quick?
            #(crit/quick-benchmark* goal params)
            #(crit/benchmark* goal params))
        f (if progress?
            #(with-progress-at-boot-info f)
            f)
        f (if debug?
            #(with-debug-at-boot-dbug f)
            f)
        f (if warn?
            #(with-warn-at-boot-warn f)
            f)]
    (bu/dbug "Running a %s benchmark for %s..."
             (if quick? "quick" "full") goal)
    (try
      (when before (before))
      (-> (f)
          (remove-criterium-types)
          (interpret-measurements))
      (finally
        (when after (after))))))

(ns tulos.boot-criterium.criterium
  (:require [clojure.walk :as walk]
            [criterium.core :as crit]
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

(defn run-goal [code params quick? progress?]
  (let [f (resolve-code code)
        f (if quick?
            #(crit/quick-benchmark* f params)
            #(crit/benchmark* f params))
        f (if progress?
            #(crit/with-progress-reporting (f))
            f)]
    (-> (f)
        (remove-criterium-types)
        (interpret-measurements))))

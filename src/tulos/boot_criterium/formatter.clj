(ns tulos.boot-criterium.formatter
  (:require [clojure.pprint :as pprint]
            [criterium.core :as crit]))

;; Result has the following format:
;;{:overhead 1.5661916171758835E-9,
;; :samples (1000725533 1001061123 1001113132 1001083433 1001392222 1001103035),
;; :runtime-details
;; {:spec-vendor "Oracle Corporation",
;;  :spec-name "Java Virtual Machine Specification",
;;  :vm-version "25.45-b02",
;;  :name "myname",
;;  :clojure-version-string "1.7.0",
;;  :java-runtime-version "1.8.0_45-b14",
;;  :java-version "1.8.0_45",
;;  :vm-name "Java HotSpot(TM) 64-Bit Server VM",
;;  :vm-vendor "Oracle Corporation",
;;  :clojure-version
;;  {:major 1,
;;   :minor 7,
;;   :incremental 0,
;;   :qualifier nil},
;;  :spec-version "1.8",
;;  :sun-arch-data-model "64",
;;  :input-arguments ["-Dboot.app.path=/usr/local/bin/boot"]},
;; :mean [1.0010797463333334 (1.0009040316666666 1.0012460443333335)],
;; :final-gc-time 617313061,
;; :execution-count 1,
;; :variance [6.436140019696764E-8 (2.0889246208168E-8 1.3334226681629684E-7)],
;; :os-details
;; {:arch "x86_64",
;;  :available-processors 4,
;;  :name "Mac OS X",
;;  :version "10.10.1"},
;; :tail-quantile 0.025,
;; :outlier-variance 0.13888888888888873,
;; :outliers
;; {:low-severe 1,
;;  :low-mild 0,
;;  :high-mild 0,
;;  :high-severe 1},
;; :warmup-time 5003128876,
;; :lower-q [1.000725533 (1.000725533 1.000773982875)],
;; :warmup-executions 5,
;; :sample-count 6,
;; :upper-q [1.00135733575 (1.00110058475 1.001392222)],
;; :total-time 6.006478478,
;; :sample-variance [4.502510251186645E-8 (0.0 0.0)],
;; :options
;; {:max-gc-attempts 100,
;;  :samples 6,
;;  :target-execution-time 100000000,
;;  :warmup-jit-period 5000000000,
;;  :tail-quantile 0.025,
;;  :bootstrap-size 500,
;;  :overhead 1.5661916171758835E-9,
;;  :verbose true},
;; :interpreted {:overhead [2.171547395E-9 [1.0E9 "ns"]], :lower-quantile [0.025 [100 "%"]], :mean [1.0009014065000001 [1 "sec"]], :variance [4.6282109530861986E-4 [1000000.0 "µs"]], :lower-q [1.000140615 [1 "sec"]], :outlier-effect :moderate, :upper-quantile [0.975 [100 "%"]], :evaluation-count 6, :upper-q [1.00139630375 [1 "sec"]], :sample-variance [4.5964378454306797E-4 [1000000.0 "µs"]], :sample-mean [1.0009054926666667 [1 "sec"]]}
;; :sample-mean [1.0010797463333334 (1.000443172753449 1.0017163199132177)],
;; :results (nil nil nil nil nil nil)}

(defn edn [results]
  (with-out-str (pprint/pprint results)))

(defn- format-estimate [[value [scale unit]]]
  (format "%.4f %s" (* value scale) unit))

(defn table [results]
  (with-out-str
    (->> results
         (map (fn [{:keys [goal result]}]
                (merge
                  result
                  {:benchmark/goal (:label goal)}
                  (-> (:interpreted result)
                      (update :mean format-estimate)
                      (update :variance format-estimate)
                      (update :lower-q format-estimate)
                      (update :upper-q format-estimate)))))
         (sort-by :benchmark/goal)
         (pprint/print-table
           [:benchmark/goal :mean :variance :upper-q :lower-q
            :evaluation-count :outlier-effect]))))

(defn criterium [results]
  (with-out-str
    (doseq [{:keys [goal result]} results]
      (println "Goal:" (:label goal))
      (crit/report-result result)
      (println ""))))

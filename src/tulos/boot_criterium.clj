(ns tulos.boot-criterium
  {:boot/export-tasks true}
  (:require [clojure.java.io :as io]
            [boot.core :as core :refer [deftask]]
            [boot.pod :as pod]))

(def ^:private criterium-deps
  '[[criterium "0.4.4"]])

(defn- ensure-out-dir [parent]
  (doto (io/file parent "criterium")
    (.mkdir)))

(defn- pod-with-deps [deps]
  (pod/make-pod (update (core/get-env) :dependencies into deps)))

(def ^:private criterium-pod
  (pod-with-deps criterium-deps))

(defn- bench-pod [deps]
  (if (seq (filter (fn [[d _]] (= d 'criterium)) deps))
    (pod-with-deps deps)
    (pod-with-deps (concat criterium-deps deps))))

(defn- cleanup-code-label [^String code]
  (-> (.replaceAll code "\\(" "_")
      (.replaceAll "\\)" "_")
      (.replaceAll "\\/" "_")
      (.replaceAll " " "_")))

(deftask bench
  "Run a benchmarck of the specified goal function.

  As of Criterium 0.4.3 `params` may take the following:

  report options:

    - `verbose`: bool - report execution information to stdout
    - `os`: bool - add OS information
    - `runtime`: bool - add Java runtime information

  benchmark options:

    - `max-gc-attempts`: int
    - `gc-before-sample`: int
    - `overhead`: double (ns) - timing loop overhead, estimated by Criterium if not provided
    - `samples`: int
    - `target-execution-time`: long (ns)
    - `warmup-jit-period`: long (ns)
    - `tail-quantile`: double
    - `bootstrap-size`: int"
  [g goal GOAL                code        "qualified name of the goal function to run or a valid form"
   l label LABEL              str         "label for the goal"
   d dependencies ID:VER      [[sym str]] "vector of goal dependencies"
   c criterium-opts KEY=VAL   edn         "options to the Criterium"
   P progress                 bool        "show progress report?"
   D debug                    bool        "show debug output?"
   W warn                     bool        "show warning output?"
   Q quick                    bool        "run a quick benchmark?"]
  (let [bench-pod (bench-pod dependencies)]
    (core/with-pre-wrap fs
      (let [label (or label (str goal))
            result (pod/with-eval-in bench-pod
                     (require '[tulos.boot-criterium.criterium])
                     (tulos.boot-criterium.criterium/run-goal
                       '~goal ~criterium-opts
                       ~{:quick? quick, :progress? progress
                         :debug? debug, :warn? warn}))
            t (core/tmp-dir!)
            out-name (str (cleanup-code-label label) ".edn")
            out-dir (ensure-out-dir t)]
        (spit (io/file out-dir out-name) (pr-str result))
        (-> fs
            (core/add-asset t)
            (core/commit!)
            (core/add-meta
              {(.getPath (io/file (.getName out-dir) out-name))
               {:benchmark/goal {:label label, :dependencies dependencies}}}))))))

;; TODO: remove when in boot: https://github.com/boot-clj/boot/pull/398
(defn- tmpfile-filter [mkpred]
  (fn [criteria files & [negate?]]
    ((if negate? remove filter)
     #(some identity ((apply juxt (map mkpred criteria)) %)) files)))

(defn- by-meta [meta-preds files & [negate?]]
  ((tmpfile-filter identity) meta-preds files negate?))

(defn- format-in [pod formatter tmp-files]
  (let [paths (mapv (fn [t] {:path (.getPath (core/tmp-file t))
                             :goal (:benchmark/goal t)})
                    tmp-files)]
    (pod/with-eval-in pod
      (require '[clojure.edn :as edn])
      (require '[tulos.boot-criterium.util :as u])
      (require '[tulos.boot-criterium.formatter])
      ((u/resolve-var '~formatter 'tulos.boot-criterium.formatter)
        (map (fn [{:keys [path goal]}]
               {:goal goal
                :result (-> (slurp path)
                            (edn/read-string))})
             '~paths)))))

(deftask report
  "Report the results of the benchmark.

  `formatter` parameter accepts a name of a function to format results with.
  The resolved function should accept a sequence of Criterium results produced
  by the `bench` task and return a string suitable for output.

  You can use one of the three default formatters:

    * `edn` - returns the vanilla Criterium results together with some
      additional goal data
    * `table` - tabulates the results
    * `criterium` - uses the Criterium built-in report functionality"
  [f formatter FORMATTER  sym  "function to format the output"
   O stdout               bool "output to stdout?"]
  (let [formatter-pod criterium-pod]
    (core/with-pre-wrap fs
      (let [formatted (->> fs
                           (core/ls)
                           (by-meta [:benchmark/goal])
                           (format-in formatter-pod formatter))]
        (if stdout
          (do (print formatted)
              fs)
          (let [dir (core/tmp-dir!)
                out-dir (ensure-out-dir dir)
                result-file (io/file out-dir "results.out")]
            (spit result-file formatted)
            (-> fs (core/add-asset dir) (core/commit!))))))))

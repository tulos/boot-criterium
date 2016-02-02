(set-env! :dependencies '[[tulos/boot-criterium "0.2.0-SNAPSHOT"]]
          :source-paths #{"src"})

(require '[tulos.boot-criterium :as criterium])

(deftask bench-all []
  (comp
    (criterium/bench :goal `goals/goal-a, :warn true, :quick true)
    (criterium/bench :goal `goals/goal-b, :debug true, :quick true)
    (criterium/bench :goal '(Thread/sleep 500), :progress true, :quick true)
    (criterium/report :formatter 'criterium, :stdout true)))

(set-env! :dependencies '[[tulos/boot-criterium "0.1.0-SNAPSHOT"]]
          :source-paths #{"src"})

(require '[tulos.boot-criterium :as criterium])

(deftask bench-all []
  (comp
    (criterium/bench :goal `goals/goal-a, :quick true)
    (criterium/bench :goal `goals/goal-b, :quick true)
    (criterium/bench :goal '(Thread/sleep 500), :quick true)
    (criterium/report :formatter 'criterium, :stdout true)))

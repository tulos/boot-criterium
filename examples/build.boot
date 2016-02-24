(set-env! :dependencies '[[tulos/boot-criterium "0.2.1-SNAPSHOT"]]
          :source-paths #{"src"})

(require '[tulos.boot-criterium :as criterium])

(deftask bench-all []
  (comp
    (criterium/bench :goal `goals/sleep-1x, :warn true, :quick true)
    (criterium/bench :goal `goals/sleep-2x, :debug true, :quick true)
    (criterium/bench :before `goals/set-up
                     :after `goals/tear-down
                     :goal `goals/sleep-1x
                     :quick true)
    (criterium/bench :goal '(Thread/sleep 500), :progress true, :quick true)
    (criterium/report :formatter 'criterium, :stdout true)))

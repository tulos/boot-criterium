(def project 'tulos/boot-criterium)
(def version "0.2.0")

(set-env! :resource-paths #{"src"}
          :dependencies '[[adzerk/bootlaces "0.1.13" :scope "test"]])

(task-options!
 pom {:project     project
      :version     version
      :description "Boot tasks that run your benchmarks!"
      :url         "https://github.com/tulos/boot-criterium"
      :scm         {:url "https://github.com/tulos/boot-criterium"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(require '[adzerk.bootlaces :refer :all])
(bootlaces! version)

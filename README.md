# boot-criterium

[](dependency)
```clojure
[tulos/boot-criterium "0.2.0"] ;; latest release
```
[](/dependency)

A set of Boot tasks to benchmark your code using
[Criterium](https://github.com/hugoduncan/criterium).

## Motivation

So, you want to benchmark some code? What do you do? Set up a new Leiningen
project, add a Criterium dependency, start the REPL, create a source file,
write your function, run the benchmark, stare at the output? What if you wanted
to benchmark a simple one-liner? Now you can!

```bash
$ boot -d tulos/boot-criterium \
    bench -l sleep-1000 -g "'(Thread/sleep 1000)" -Q -- \
    bench -l sleep-500 -g "'(Thread/sleep 500)" -Q -- \
    report -f table -O

| :benchmark/goal |       :mean |   :variance |    :upper-q |    :lower-q | :evaluation-count | :outlier-effect |
|-----------------+-------------+-------------+-------------+-------------+-------------------+-----------------|
|      sleep-1000 |  1.0007 sec | 438.2283 µs |  1.0011 sec |  1.0002 sec |                 6 |       :moderate |
|       sleep-500 | 500.7362 ms | 449.0227 µs | 501.1333 ms | 500.2216 ms |                 6 |       :moderate |
```

Voila! We've just run a `criterium/quick-benchmark` with the default options
and made sure that sleeping for 500 millis takes about half a second and
sleeping for 1000 millis takes about twice as long.

Usually we want to benchmark how different approaches to the same problem
perform in comparison. Criterium doesn't provide an output format that would be
amenable to a quick comparison though. Neither does it provide the guidance on
how to setup the benchmark cases. Admittedly, it's just Clojure code, but it's
still Clojure code that you have to write.

There's the [Perforate](https://github.com/davidsantiago/perforate)
Leiningen-based project that provides a benchmarking framework. You write
goals, usually in separate namespaces, and construct cases (variants) with
different methods of achieving these goals as multimethods. `boot-criterium`
borrows the concept of a *goal* and the customizable output formats, nothing
more.

Currently `boot-criterium` will not provide any kind of structure above the
`bench` task.  You'll have to write the Clojure code yourself. It's early days
though. We'll see if some tooling emerges through use that we'll want to
include in this project.

## Usage

You've already seen how a snippet of code can be run from the command line. The
rest of this document will assume we're running in the Boot REPL.

You can run a single benchmark like so:

```clojure
(bench :goal 'my/function)
```

This will add a file with the EDN result of the benchmark to the fileset (try
running Criterium to see the result map for yourself, AFAIK it's not documented
anywhere).  To make use of the result you either need to output the fileset to
the filesystem using the Boot's built-in `target` task or use the `report` task
provided here:

```clojure
(report :format 'table, :stdout true)
```

The nice thing about `report` is that it operates on the results of all the
preceding `bench` tasks:

```clojure
(boot
  (bench :goal 'my/function-1)
  (bench :goal 'my/function-2)
  (report :format 'table)
  (target))
```

note that we didn't specify the `stdout` option to the `report` task. The above
will write the resulting fileset into the `target/` folder:

```bash
$ tree target

target/
├── criterium
│   ├── my_function-1.edn
│   ├── my_function-2.edn
│   └── results.out

$ cat target/criterium/results.out

| :benchmark/goal |       :mean |   :variance |    :upper-q |    :lower-q | :evaluation-count | :outlier-effect |
|-----------------+-------------+-------------+-------------+-------------+-------------------+-----------------|
|   my/function-1 |  1.0007 sec | 349.2924 µs |  1.0011 sec |  1.0003 sec |                 6 |       :moderate |
|   my/function-2 | 500.8664 ms | 376.1524 µs | 501.1621 ms | 500.3924 ms |                 6 |       :moderate |
```

### Dependencies

Now let's try something interesting - suppose you want to benchmark some code
with different dependency versions. Let's try running a simple `(reduce +
(range 1000))` across Clojure 1.6, 1.7 and 1.8:

```clojure
(def code '(reduce + (range 1000)))

(boot
  (bench :label "1.6", :goal code, :dependencies '[[org.clojure/clojure "1.6.0"]])
  (bench :label "1.7", :goal code, :dependencies '[[org.clojure/clojure "1.7.0"]])
  (bench :label "1.8", :goal code, :dependencies '[[org.clojure/clojure "1.8.0"]])
  (report :format 'table, :stdout true))

| :benchmark/goal |      :mean |   :variance |   :upper-q |   :lower-q | :evaluation-count | :outlier-effect |
|-----------------+------------+-------------+------------+------------+-------------------+-----------------|
|             1.6 | 37.9297 µs |   1.6069 µs | 41.9038 µs | 36.2114 µs |           1632780 |       :moderate |
|             1.7 | 11.0225 µs | 275.3703 ns | 11.5597 µs | 10.5775 µs |           6743580 |       :moderate |
|             1.8 |  9.7970 µs | 476.5232 ns | 10.8728 µs |  8.9932 µs |           6468900 |       :moderate |
```

You'd have to start three REPLs to do the same without Boot!

For complete documentation see:

```clojure
(doc bench)
```

and

```clojure
(doc report)
```

for the full documentation.

## License

Copyright © 2016 Tulos Capital

Distributed under the Eclipse Public License.

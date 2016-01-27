(ns tulos.boot-criterium.util)

(defn resolve-var
  ([sym fallback-ns]
   (if (namespace sym)
     (resolve-var sym)
     (resolve-var (symbol (name fallback-ns) (name sym)))))
  ([sym]
   (require (symbol (namespace sym)) :reload)
   (let [var (resolve sym)]
     (assert var (format "Could not resolve symbol: %s!" sym))
     var)))


(ns google-cloud.core
  (:require
    [google-cloud.pubsub :as pubsub])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main [& args]
  (println "**** in main!")
  (pubsub/get-messages-clear "test" 5 nil))
  ;(clojure.pprint/pprint (db/get-books))
  ;(clojure.pprint/pprint (take 10 (db/get-reviews-rf-all))))
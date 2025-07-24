(ns google-cloud.storage-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [cheshire.core :as cheshire]
            [google-cloud.storage :as storage]))

(def temp-file (java.io.File/createTempFile "gcs-test" ".tmp"))
(def test-data "google-cloud.storage_test")

; hack, because I can't get env to work
(def ENV (-> (slurp "profiles.clj")
           (read-string)))

(deftest upload-download
  (testing "uploading a file to Google Storage"
    (spit temp-file test-data)
    (let [
          response (storage/upload-file temp-file (-> ENV :google-storage-bucket-name))
          body     (cheshire/parse-string (:body @response))]
      (is (= true (true? (boolean (get body "mediaLink")))))))
  (testing "downloading a file from Google Storage"
    (let [
          response (storage/download-file (.getName temp-file)
                     (-> ENV :google-storage-bucket-name))
          body     (:body @response)]
      (def BODY body)
      (is (= true (not (string? body))))
      (if-not (string? body)
        (is (= test-data (apply str (map char (.bytes (:body @response))))))))))

(comment
  (-> ENV :google-storage-bucket-name)
  (-> env :google-storage-bucket-name)
  0)
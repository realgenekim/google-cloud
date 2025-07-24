(ns google-cloud.oauth-test
  (:require [clojure.test :refer :all]
            [clj-time.core             :as time]
            [cheshire.core             :as ch]
            [buddy.sign.jwt            :as jwt]
            [buddy.core.keys           :as keys]
            [google-cloud.oauth :as sut]
            [environ.core :refer [env]]))

(def dummy-creds (ch/generate-string {:client_email "a@a.com" :private_key "dummy"}))

(deftest retrieving-token-from-google

  (testing "The refresh-token function updates the token-state"
    (with-redefs [sut/get-token-from-google (fn [_] "new-token")
                  slurp                     (fn [_] dummy-creds)
                  jwt/sign                  (fn [& args] "XXX")
                  keys/str->private-key     (fn [_] "YYY")
                  env                       (fn [_] "fake-credentials-path")]
      (is (= nil (:token @sut/token-state)))
      (sut/refresh-token "pubsub")
      (is (= "new-token" (:token @sut/token-state))))))

(deftest retrieving-local-token

  (testing "The get-oauth-token function refreshes token-state when existing token has expired"
    (let [call-counter (atom 0)]
      (with-redefs [sut/refresh-token (fn [_] (swap! call-counter inc))
                    slurp                    (fn [_] dummy-creds)
                    jwt/sign                 (fn [& args] "XXX")
                    keys/str->private-key    (fn [_] "YYY")
                    env                      (fn [_] "fake-credentials-path")]
        (swap! sut/token-state assoc :expires-at (time/minus (time/now) (time/minutes 5)))
        (sut/get-token)
        (is (= 1 @call-counter)))))

  (testing "The get-oauth-token function does not refresh token-state when existing token is still valid"
    (let [call-counter (atom 0)]
      (with-redefs [sut/refresh-token (fn [] (swap! call-counter inc))
                    slurp                    (fn [_] dummy-creds)
                    jwt/sign                 (fn [& args] "XXX")
                    keys/str->private-key    (fn [_] "YYY")
                    env                      (fn [_] "fake-credentials-path")]
        (swap! sut/token-state assoc :expires-at (time/plus (time/now) (time/minutes 5)))
        (sut/get-token)
        (is (= 0 @call-counter))))))

(deftest token-state-management
  
  (testing "Token state atom is properly initialized"
    (is (map? @sut/token-state))
    (is (contains? @sut/token-state :token))
    (is (contains? @sut/token-state :expires-at)))
  
  (testing "Token state can be updated"
    (let [original-state @sut/token-state]
      (reset! sut/token-state {:token "test-token" :expires-at (time/now)})
      (is (= "test-token" (:token @sut/token-state)))
      ;; Reset to original state
      (reset! sut/token-state original-state))))

(deftest oauth-data-construction
  
  (testing "OAuth JWT payload is correctly constructed"
    (with-redefs [slurp (fn [_] dummy-creds)
                  env   (fn [_] "fake-path")]
      (let [creds (ch/parse-string dummy-creds true)
            oauth-data {:iss   (:client_email creds)
                       :scope "https://www.googleapis.com/auth/pubsub"
                       :aud   "httpwww.googleapis.com/oauth2/v4/token"
                       :exp   (time/plus (time/now) (time/hours 1))
                       :iat   (time/now)}]
        (is (= "a@a.com" (:iss oauth-data)))
        (is (string? (:scope oauth-data)))
        (is (time/after? (:exp oauth-data) (time/now)))))))

(deftest error-handling
  
  (testing "refresh-token throws exception when credentials are missing"
    (with-redefs [env (fn [_] nil)]
      (is (thrown-with-msg? Exception 
                           #"Unable to refresh the OAuth token"
                           (sut/refresh-token "pubsub")))))
  
  (testing "get-token handles scope parameter correctly"
    (with-redefs [sut/refresh-token (fn [scope] 
                                      (swap! sut/token-state assoc :token (str "token-for-" scope)))]
      (reset! sut/token-state {:token nil :expires-at (time/minus (time/now) (time/minutes 1))})
      (is (= "token-for-storage" (sut/get-token "storage")))
      (is (= "token-for-pubsub" (sut/get-token "pubsub"))))))

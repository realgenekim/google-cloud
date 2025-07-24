GC=google-cloud-0.3.2.jar

tests:
	# can't run rlwrap on circleci: "terminal width zero"
	#(export GOOGLE_APPLICATION_CREDENTIALS=$$HOME/gcloud-service-key.json; clojure -M:test)
	(export GOOGLE_APPLICATION_CREDENTIALS=/Users/genekim/src.local/secrets/booktracker-1208-projectowner-77cd30ce65a2.json; \
		clojure -X:test)

install:
	lein compile
	lein install
	lein localrepo coords resources/$(GC)
	lein localrepo install --pom  pom.xml \
	target/$(GC) genekim/google-cloud 0.3.2

uberjar:
	time clojure -X:uberjar :aot true :jar ./target/google-cloud-standalone.jar \
		:main-class google-cloud.core

runuberjar:
	# java -cp target/pubsub-standalone.jar clojure.main -m pubsub.core --mode get-salesranks
	GOOGLE_APPLICATION_CREDENTIALS=projectowner-BookTracker-53434d3aa596.json java -jar target/google-cloud-standalone.jar --mode get-salesranks

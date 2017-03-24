# memcache_loadtest
================================

Sample application that tests AppEngine Memcache access from AppEngine Flex VMs,
through AppEngine Memcache client and spymemcached client.

Deploy in AppEngine Flex

    mvn gcloud:deploy

Run gcloud init for project and location to deploy to. Modify Dockerfile for VM
docker image to use. Modify appengine-web.xml for other AppEngine Flex support
parameters.

Run memcached conformance tests

    java -jar bin/spymemcached_conformance_test.jar -s server -p port -v 1.4.22


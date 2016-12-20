# memcache_loadtest
================================

Sample application that tests AppEngine Memcache access from AppEngine Flex VMs,
through AppEngine Memcache client and spymemcached client.

It requires maven artifact memcache-transcoder, which can be built from
https://github.com/yubin154/memcache_transcoder

    git clone https://github.com/yubin154/memcache_transcoder
    cd memcache_transcoder
    mvn clean install

Deploy in AppEngine Flex

    mvn gcloud:deploy

Run gcloud init for project and location to deploy to. Modify Dockerfile for VM
docker image to use. Modify appengine-web.xml for other AppEngine Flex support
parameters.

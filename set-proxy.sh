#!/bin/bash

if [[ ! -z "$http_proxy_host" ]]; then
    export GRADLE_OPTS="$GRADLE_OPTS -Dhttp.proxyHost=$http_proxy_host -Dhttp.proxyPort=$http_proxy_port"
fi

if [[ ! -z "$https_proxy_host" ]]; then
    export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyHost=$https_proxy_host -Dhttps.proxyPort=$https_proxy_port"
fi

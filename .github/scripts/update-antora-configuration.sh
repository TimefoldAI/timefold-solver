#!/bin/bash

sed -i "s/\${project\.version}b0/$LATEST_VERSION/g" $1
sed -i "s/\${project\.version}/$LATEST_VERSION/g" $1
sed -i "s/\${maven\.compiler\.release}/$JAVA_VERSION/g" $1
sed -i "s/\${maven\.min\.version}/$MAVEN_VERSION/g" $1
sed -i "s/\${version\.io\.quarkus}/$QUARKUS_VERSION/g" $1
sed -i "s/\${version\.org\.springframework\.boot}/$SPRING_VERSION/g" $1
sed -i "s/\${version\.ch\.qos\.logback}/$LOGBACK_VERSION/g" $1
sed -i "s/\${version\.exec\.plugin}/$EXEC_MAVEN_VERSION/g" $1
sed -i "s/\${version\.rewrite\.plugin}/$REWRITE_PLUGIN_VERSION/g" $1
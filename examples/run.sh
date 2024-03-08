#!/bin/bash
# Requires SDKMAN to be installed.
# Assumes SDKMAN placed itself in .bashrc.

export MAVEN_OPTS="-XX:+UseParallelGC -Xmx2g"
source ~/.bashrc

#sdk default java 17.0.10-tem
#for i in {1..10}  ; do
#  echo "Running jdk17-$i"
#  mvn exec:java
#done

sdk default java 21.0.2-tem
for i in {1..10}  ; do
  echo "Running jdk21-$i"
  mvn exec:java
done

sdk default java 22.ea.36-open
for i in {1..10}  ; do
  echo "Running jdk22-$i"
  mvn exec:java
done

#GraalVM EA builds are not in SDKMAN.
for i in {1..10}  ; do
  echo "Running graalvm22-$i"
  JAVA_HOME="/home/triceo/Apps/graalvm-jdk-22+36.1/" mvn exec:java
done

cd local/data/general
for d in */ ; do
    cd $d
    echo "===== Results for $d ====="
    cat index.html|grep "/s</td>"|sed 's/^ *//;s/ *$//'|sed 's/<td>//;s/<\/td>//'|uniq
    cd ..
done
cd ../../..

sdk default java 21.0.2-tem
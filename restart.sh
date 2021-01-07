#!/bin/bash

kill -9 $(ps | grep java | awk '{print $1}')

./gradlew clean build

java -jar build/libs/dev-service-broker-1.0.0-SNAPSHOT.jar &

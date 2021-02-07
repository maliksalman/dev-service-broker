#!/bin/bash

kill -9 $(ps | grep java | awk '{print $1}')

./gradlew clean build

SPRING_PROFILES_ACTIVE=debug java -jar build/libs/dev-service-broker-1.0.0-SNAPSHOT.jar &

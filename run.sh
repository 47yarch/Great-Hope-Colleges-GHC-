#!/usr/bin/env sh
set -eu
mvn clean package
mvn exec:java

#!/bin/sh

set -e

# TODO: Lie to testcontainers that local custom image is the real one
docker build . -t postgresql:12.1-tableversion-1.7.1
./mvnw clean verify "$@"

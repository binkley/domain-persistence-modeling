#!/bin/sh

set -e

# TODO: Lie to testcontainers that local custom image is the real one
docker build . -t postgresql:12.1-tableversion-1.8.0
./mvnw clean verify "$@"

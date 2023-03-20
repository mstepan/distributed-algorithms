#!/usr/bin/env bash

docker run \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d --rm --name distibuted-lock-postgres \
  postgres:15

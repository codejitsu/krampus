#!/bin/bash

sleep 30

cqlsh cassandra-seed-node -f /init/schema/cassandra-schema.cql
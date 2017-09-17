#!/bin/bash

sleeptime=60

echo "Waiting for cassandra node... Sleeping time: $sleeptime"

sleep $sleeptime
bash /docker-entrypoint.sh cassandra -f
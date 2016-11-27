#!/bin/bash

response=$(curl -sL -w "%{http_code}\\n" "http://admin:admin@statsd:80/api/datasources" -o /dev/null)

while [ $response != 200 ]; do
    echo "Grafana is not up...${response}"
    sleep 1
    response=$(curl -sL -w "%{http_code}\\n" "http://admin:admin@statsd:80/api/datasources" -o /dev/null)
done

curl -v 'http://admin:admin@statsd:80/api/datasources' \
    -X POST -H "Content-Type: application/json" \
    --data-binary <<DATASOURCE \
      '{
        "name":"krampus",
        "type":"graphite",
        "url":"http://localhost:8000",
        "isDefault":true,
        "access":"proxy"
      }'
DATASOURCE
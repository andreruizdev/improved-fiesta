#!/bin/bash
set -e

echo "Starting Local MLOps Infrastructure..."
docker-compose up -d

echo "Infrastructure is spinning up. To initialize Debezium CDC:"
echo ""
echo "curl -i -X POST -H 'Accept:application/json' -H 'Content-Type:application/json' localhost:8083/connectors/ -d '{
  \"name\": \"outbox-connector\",
  \"config\": {
    \"connector.class\": \"io.debezium.connector.postgresql.PostgresConnector\",
    \"tasks.max\": \"1\",
    \"database.hostname\": \"postgres\",
    \"database.port\": \"5432\",
    \"database.user\": \"postgres\",
    \"database.password\": \"postgres\",
    \"database.dbname\" : \"fiesta\",
    \"database.server.name\": \"dbserver1\",
    \"table.include.list\": \"public.outbox_events\",
    \"tombstones.on.delete\" : \"false\",
    \"plugin.name\": \"pgoutput\"
  }
}'"

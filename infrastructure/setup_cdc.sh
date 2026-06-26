#!/bin/bash

# Post-Deployment Execution Command to Provision the Outbox Connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  http://localhost:8083/connectors/ -d '{
  "name": "foundry-outbox-cdc",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres-service",
    "database.port": "5432",
    "database.user": "cdc_user",
    "database.password": "VaultedSecurePassword",
    "database.dbname" : "foundry",
    "database.server.name": "foundry_cluster",
    "table.include.list": "public.outbox_events",
    "tombstones.on.delete" : "false",
    "plugin.name": "pgoutput"
  }
}'
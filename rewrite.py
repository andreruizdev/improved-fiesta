with open("platform-infra/local-cluster.sh", "w") as f:
    f.write("""#!/bin/bash
set -e

echo "Starting Local MLOps Infrastructure..."
docker-compose up -d

read -p "Enter Azure Key Vault Name: " AKV_NAME
if [ -z "$AKV_NAME" ]; then
    echo "Error: Azure Key Vault Name is required."
    ex""" + """it 1
fi

read -p "Enter Azure Secret Name: " AKV_SECRET_NAME
if [ -z "$AKV_SECRET_NAME" ]; then
    echo "Error: Azure Secret Name is required."
    ex""" + """it 1
fi

echo "Fetching database password from Azure Key Vault..."
DB_PASSWORD=$(az keyvault secret show --vault-name "$AKV_NAME" --name "$AKV_SECRET_NAME" --query value -o tsv)
if [ -z "$DB_PASSWORD" ]; then
    echo "Error: Failed to retrieve database password or password is empty."
    ex""" + """it 1
fi

echo "Infrastructure is spinning up. To initialize Debezium CDC:"
echo ""
cat <<CURL_EOF
curl -i -X POST -H 'Accept:application/json' -H 'Content-Type:application/json' localhost:8083/connectors/ -d '{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "${DB_PASSWORD}",
    "database.dbname" : "fiesta",
    "database.server.name": "dbserver1",
    "table.include.list": "public.outbox_events",
    "tombstones.on.delete" : "false",
    "plugin.name": "pgoutput"
  }
}'
CURL_EOF
""")

## Scratchpad

`docker build -t mauricio/kafka-plain .`
TODO Add maven plugin to build the above

Add users to vault

For admin
`vault kv put secret/kafka/admin username=admin password=admin`
For other users
`vault kv put secret/kafka/users alice=alicepwd bob=bobpwd admin=adminpwd`

### ENVARS
```bash
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="root-token"

```

### Add to kafka server
Into your `server.properties`
`listener.name.sasl_plaintext.plain.sasl.server.callback.handler.class=com.ultimatesoftware.dataplatform.vaultjca.VaultAuthenticationLoginCallbackHandler`

### Starting server
```bash
export KAFKA_OPTS="-Djava.security.auth.login.config=/Users/mauricioa/git/dataplatform/kafka-vault-jca/src/test/resources/kafka_server_vault_jaas.conf -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
```

### Starting consumers
```bash
export KAFKA_OPTS="-Djava.security.auth.login.config=/Users/mauricioa/git/dataplatform/kafka-vault-jca/src/test/resources/kafka_client_alice_jass.conf -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006"
```
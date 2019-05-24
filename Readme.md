## Scratchpad

`docker build -t mauricio/kafka-plain .`
TODO Add maven plugin to build the above

Add users to vault

For admin
`vault kv put secret/kafka/admin username=admin password=admin`
For other users
`vault kv put secret/kafka/users alice=alicepwd bob=bobpwd admin=adminpwd`

ENVARS
```bash
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="root-token"

```
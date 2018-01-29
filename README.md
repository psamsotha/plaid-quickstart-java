# Plaid Quickstart for Java

### Build

```bash
mvn package
```

### Run

```bash
PLAID_PUBLIC_KEY=[PUBLIC_KEY] \
PLAID_CLIENT_ID=[CLIENT_ID] \
PLAID_SECRET=[SECRET] \
PLAID_ENV=sandbox \
mvn spring-boot:run
```

### Play

Visit `http://localhost:8080`

User credentials `user_good` and `pass_good`.

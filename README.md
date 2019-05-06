# HDS-notary-server

### Execute tests and server
```bash
mvn clean install exec:java
```

### Start replica 0
```bash
mvn clean install exec:java -Dexec.args=0
```

### In another terminal replica 1
```bash
mvn exec:java -Dexec.args=1
```


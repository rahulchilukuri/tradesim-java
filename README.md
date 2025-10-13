# tradesim-java

Java port of the tradesim project (core logic only, no plotting). Uses virtual threads (Java 21+) for Monte Carlo parallelism.

Build
```
cd /path/to/tradesim-java
mvn package
```

Run
```
java -jar target/tradesim-java-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Notes
- Requires Java 21 or later to run virtual threads.
- Batch size and number of simulations are configured in `Config.montyConfig()`.

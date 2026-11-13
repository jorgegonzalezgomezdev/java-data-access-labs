# Java data access labs

Three Maven projects showing how Java talks to files and databases, from plain JDBC to JPA.

| Project | What it shows |
|---|---|
| [`01-jdbc-sqlite-crud`](01-jdbc-sqlite-crud) | Layered CRUD (model / service / controller) with JDBC, `PreparedStatement` and SQLite, tested with JUnit 5 |
| [`02-file-processing-biodam`](02-file-processing-biodam) | Reading and validating CSV data files (valid vs malformed records) with Java I/O |
| [`03-jdbc-vs-jpa`](03-jdbc-vs-jpa) | Customers and orders persisted twice, with plain JDBC and with JPA/Hibernate, against MySQL in Docker |

Each folder opens in IntelliJ (File → Open on the folder with the `pom.xml`) and runs with `mvn test`.

Built during the *Data Access* module (Multiplatform App Development, Nebrija, 2026).

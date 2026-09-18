# Blocking Spring MVC with Spring Data JDBC and virtual threads, not reactive

Astronauth runs on Spring MVC with blocking persistence through Spring Data JDBC, packaged as a single executable jar, with virtual threads enabled on Java 25. Spring Authorization Server only supports the servlet stack and its stores use JDBC, as does Spring Session; a reactive persistence layer next to them would mean two connection pools and no shared transactions. Virtual threads give blocking code the I/O scalability that used to require reactive programming, and our real limits (password hashing, database connections, token signing) are the same in either model.

## Considered Options

- **R2DBC with coroutines (the original setup):** no scalability gain for request/response work over a database, and it cannot share transactions with Spring Authorization Server or Spring Session.
- **JPA:** its lazy loading, dirty checking and proxies work against ADR 0001's explicit mapping between persistence rows and domain objects; Spring Data JDBC keeps that mapping plain.
- **Java 21:** supports virtual threads, but pins them inside `synchronized` blocks; Java 25 (JEP 491) removes that.

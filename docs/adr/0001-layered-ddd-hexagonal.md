# Layered DDD packages inside a hexagonal architecture

Each bounded context (today only `account`) is organised by layer: domain, application, api and infrastructure, with features as subpackages inside a layer. The domain is modelled with tactical DDD (aggregates, value objects, repositories), and hexagonal (ports and adapters) rules govern dependencies. We prefer this explicit structure: every rule has one obvious home, each layer has one job, and the layer boundaries are visible in the package names, so tests can enforce them.

```
account/
├── domain/                 # aggregates, value objects, repository interfaces
├── application/            # ports the use cases need (EmailSender, PasswordHasher)
│   └── registration/       # one feature: its use cases and feature-specific ports
├── api/
│   └── graphql/            # inbound adapters per transport (later api/rest/)
└── infrastructure/         # outbound adapters and the Spring wiring
    ├── persistence/
    ├── email/
    └── ...
```

## Rules

- **Dependencies point inward:** api and infrastructure → application → domain. Nothing depends on api or infrastructure. `ArchitectureTests` enforces this.
- **The domain and application layers use only the standard library:** no Spring, persistence, GraphQL or logging imports. `ArchitectureTests` enforces this too.
- **The domain holds concepts and rules, not capabilities.** Aggregates, value objects and repository interfaces (a repository is the domain's idea of "all Accounts") live in `domain/`. Ports for things the application does, such as sending email, hashing passwords or queueing work, are owned by `application/`: shared ones at its root, feature-specific ones in the feature's package.
- **The domain receives time, identifiers and randomness as values.** Use cases read the clock, generate IDs and tokens, and pass them in, so domain behaviour is deterministic and tested through its outputs.
- **Use cases are thin orchestrators.** Decisions belong in the domain; a use case loads, calls the domain, stores and triggers side effects.
- **Inbound adapters** in `api/` are thin: they map transport input to a use case and map the result back. They contain no business rules.
- **Outbound adapters** in `infrastructure/` implement the ports (Spring Data JDBC, Argon2, SMTP). Persistence rows are separate from domain objects and are mapped at the adapter boundary.

## Considered Options

- **Feature-first (each feature a package holding its use case and inbound adapters, next to shared `domain/` and adapter packages):** a feature reads in one folder, but the layers are only a convention. After one feature, `account/` already held four sibling adapter packages beside `domain/` and `registration/`, with nothing in the structure saying which were infrastructure. We started with this and moved away from it.
- **Slices without layers (controller, logic and SQL mixed per feature):** fewer files, but no framework-free domain and no clear home for rules.
- **Full clean architecture (input/output ports, presenters per use case):** skipped for now; the use-case classes carry most of its value.
- **CQRS:** not needed. GraphQL already separates queries from mutations, and a future REST API is just another inbound adapter.
- **Event sourcing:** not needed; current-state tables plus an audit log.

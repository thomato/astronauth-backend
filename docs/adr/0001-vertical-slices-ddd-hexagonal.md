# Vertical slices with tactical DDD inside a hexagonal architecture

Our own domain code is organised slice-first: each feature (e.g. registration, email verification) is its own package holding that feature's use case and inbound adapters. The domain is modelled with tactical DDD (aggregates, value objects, domain errors), and hexagonal (ports and adapters) rules govern dependencies. We prefer this explicit structure: every rule has one obvious home, each layer has one job, and a feature can be read in one folder.

```
account/
├── domain/          # aggregates, value objects, outbound ports (shared by all slices)
├── persistence/     # outbound adapters for the domain's ports (shared)
└── registration/    # one slice: use case + inbound adapters (GraphQL, later REST)
```

## Rules

- **Dependencies point inward:** adapters → use case → domain. The domain and use cases have no Spring, persistence or GraphQL imports.
- **Aggregates are shared, not owned by a slice.** An aggregate used by several features lives in the context's `domain/`, together with its outbound ports; the adapters implementing those ports live in shared packages such as `persistence/`.
- **Outbound ports** are interfaces owned by the domain or a use case (e.g. the user repository, password hashing, sending email). **Outbound adapters** implement them (R2DBC, BCrypt, SMTP).
- **Inbound adapters** (GraphQL now, REST later) live in their slice and are thin: they map transport input to the use case and map the result back. They contain no business rules.
- Persistence entities are separate from domain objects and are mapped at the outbound adapter boundary.

## Considered Options

- **Layer-first (`domain/`, `application/`, `adapter/` at the top, features only inside `application/`):** equally hexagonal, but spreads one feature across the tree.
- **Slices without layers (controller, logic and SQL mixed per feature):** fewer files, but no framework-free domain and no clear home for rules.
- **Full clean architecture (input/output ports, presenters per use case):** skipped for now; the use-case classes carry most of its value.
- **CQRS:** not needed. GraphQL already separates queries from mutations, and a future REST API is just another inbound adapter.
- **Event sourcing:** not needed; current-state tables plus an audit log.

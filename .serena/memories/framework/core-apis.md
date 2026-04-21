# Framework Core APIs

## Facade Interfaces

### org.moqui.context
| Interface | Purpose |
|-----------|---------|
| `ExecutionContext` | Central access point |
| `UserFacade` | Authentication/Authorization |
| `ServiceFacade` | Service calls |
| `EntityFacade` | Data access |
| `ScreenFacade` | Screen rendering |
| `MessageFacade` | Validation messages |
| `CacheFacade` | Caching |
| `TransactionFacade` | Transactions |
| `L10nFacade` | Localization |

### org.moqui.entity
| Interface | Purpose |
|-----------|---------|
| `EntityValue` | Single entity record |
| `EntityList` | List of entities |
| `EntityFind` | Query builder |
| `EntityCondition` | Query conditions |

### org.moqui.service
| Interface | Purpose |
|-----------|---------|
| `ServiceCall` | Service call builder |
| `ServiceCallSync` | Sync calls |
| `ServiceCallAsync` | Async calls |

## Service Definitions

Services are defined in XML files with pattern: `verb#Noun`

Example: `create#LetterOfCredit`

## Entity Definitions

Entities defined in `*.xml` files under `entity/` directories.

Pattern: `<entity entity-name="EntityName" package="org.example">`

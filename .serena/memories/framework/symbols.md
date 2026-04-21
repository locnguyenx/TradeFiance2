# Framework Symbol Overview

## Java Interfaces (42)

### org.moqui.context
| Interface | Extends |
|-----------|---------|
| `ExecutionContextFactory` | |
| `ExecutionContext` | |
| `UserFacade` | |
| `ServiceFacade` | |
| `EntityFacade` | |
| `ScreenFacade` | |
| `MessageFacade` | |
| `CacheFacade` | |
| `TransactionFacade` | |
| `L10nFacade` | |
| `ResourceFacade` | |
| `WebFacade` | |
| `ArtifactExecutionFacade` | |
| `ArtifactExecutionInfo` | |
| `LoggerFacade` | |
| `ElasticFacade` | |
| `NotificationMessage` | `Serializable` |
| `NotificationMessageListener` | |
| `LogEventSubscriber` | |
| `ScriptRunner` | |
| `TemplateRenderer` | |
| `ToolFactory<V>` | |
| `TransactionInternal` | |

### org.moqui.entity
| Interface | Extends |
|-----------|---------|
| `EntityValue` | `Map<String,Object>`, `Externalizable`, `Comparable<EntityValue>`, `Cloneable`, `SimpleEtl.Entry` |
| `EntityList` | `List<EntityValue>`, `Iterable<EntityValue>`, `Cloneable`, `RandomAccess`, `Externalizable` |
| `EntityFind` | `Serializable`, `SimpleEtl.Extractor` |
| `EntityCondition` | `Externalizable` |
| `EntityConditionFactory` | |
| `EntityListIterator` | `ListIterator<EntityValue>`, `AutoCloseable` |
| `EntityDynamicView` | |
| `EntityDataWriter` | |
| `EntityDataLoader` | |
| `EntityDatasourceFactory` | |

### org.moqui.service
| Interface | Extends |
|-----------|---------|
| `ServiceCall` | |
| `ServiceCallSync` | `ServiceCall` |
| `ServiceCallAsync` | `ServiceCall` |
| `ServiceCallSpecial` | `ServiceCall` |
| `ServiceCallJob` | `ServiceCall`, `Future<Map<String,Object>>` |
| `ServiceCallback` | |

### org.moqui.screen
| Interface | Extends |
|-----------|---------|
| `ScreenRender` | |
| `ScreenFacade` | |
| `ScreenTest` | |

### org.moqui.util
| Interface | Extends |
|-----------|---------|
| `SimpleTopic<E>` | |

### org.moqui.etl
| Interface | Description |
|-----------|-------------|
| `Entry` | ETL Entry |
| `Extractor` | Data Extractor |
| `Transformer` | Data Transformer |
| `Loader` | Data Loader |

## Java Classes (38)

### Root org.moqui
| Class | Extends |
|-------|---------|
| `Moqui` | |
| `BaseException` | `RuntimeException` |
| `BaseArtifactException` | `BaseException` |

### org.moqui.context
| Class | Extends |
|-------|---------|
| `PasswordChangeRequiredException` | `CredentialsException` |
| `ArtifactTarpitException` | `BaseArtifactException` |
| `AuthenticationRequiredException` | `BaseArtifactException` |
| `WebMediaTypeException` | `BaseArtifactException` |
| `TransactionException` | `BaseArtifactException` |
| `MessageFacadeException` | `BaseArtifactException` |
| `ArtifactAuthorizationException` | `BaseArtifactException` |
| `SecondFactorRequiredException` | `AuthenticationException` |
| `ValidationError` | `BaseArtifactException` |
| `MoquiLog4jAppender` | `AbstractAppender` (final) |

### org.moqui.util
| Class | Implements |
|-------|------------|
| `LiteStringMap<V>` | `Map<String,V>`, `Externalizable`, `Comparable<Map<String,? extends V>>`, `Cloneable` |
| `MClassLoader` | `ClassLoader` |
| `ContextStack` | `Map<String,Object>` |
| `CollectionUtilities` | |
| `ContextBinding` | `Binding` |
| `WebUtilities` | |
| `StringUtilities` | |
| `SystemBinding` | `Binding` |
| `ObjectUtilities` | |
| `MNode` | `TemplateNodeModel`, `TemplateSequenceModel`, `TemplateHashModelEx`, `AdapterTemplateModel`, `TemplateScalarModel` |
| `RestClient` | |

### org.moqui.entity
| Class | Extends |
|-------|---------|
| `EntityValueNotFoundException` | `EntityException` |
| `EntityNotFoundException` | `EntityException` |
| `EntityException` | `BaseException` |

### org.moqui.service
| Class | Extends |
|-------|---------|
| `ServiceException` | `BaseException` |

### org.moqui.jcache
| Class | Extends/Implements |
|-------|--------------------|
| `MEntry<K,V>` | `Cache.Entry<K,V>` |
| `MStats` | `CacheStatisticsMXBean` |
| `MCache<K,V>` | `Cache<K,V>` |
| `MCacheConfiguration<K,V>` | `MutableConfiguration<K,V>` |
| `MCacheManager` | `CacheManager` |

### org.moqui.resource
| Class | Extends |
|-------|---------|
| `ResourceReference` | `Serializable` (abstract) |
| `UrlResourceReference` | `ResourceReference` |
| `ClasspathResourceReference` | `UrlResourceReference` |

### org.moqui.etl
| Class | Implements |
|-------|------------|
| `SimpleEtl` | |
| `FlatXmlExtractor` | `SimpleEtl.Extractor` |

## Groovy Implementation Classes (130 files)

### Context Implementations
- `ExecutionContextFactoryImpl` - Main factory, component loading
- `ExecutionContextImpl` - Context access
- `UserFacadeImpl` - User authentication/authorization
- `ServiceFacadeImpl` - Service calls, SECA rules
- `EntityFacadeImpl` - Entity operations, datasources
- `MessageFacadeImpl` - Validation messages
- `CacheFacadeImpl` - Caching operations
- `TransactionFacadeImpl` - Transaction management
- `TransactionInternalBitronix` - Bitronix TM integration
- `L10nFacadeImpl` - Localization
- `ResourceFacadeImpl` - Resource access
- `WebFacadeImpl` - Web request/response
- `LoggerFacadeImpl` - Logging
- `ArtifactExecutionFacadeImpl` - Artifact authorization
- `ElasticFacadeImpl` - Elasticsearch
- `NotificationMessageImpl` - Notifications

### Entity Implementations
- `EntityDefinition` - Entity metadata, relationships
- `EntityValueBase` - Base entity value
- `EntityValueImpl` - Entity value implementation
- `EntityListImpl` - Entity list
- `EntityListIteratorImpl` - List iterator
- `EntityFindImpl` - Query builder
- `EntityFindBase` - Base find
- `EntityFacadeImpl` - Entity facade
- `EntityDynamicViewImpl` - Dynamic views
- `EntityConditionFactoryImpl` - Conditions
- `EntityCache` - Entity caching
- `EntityDataLoaderImpl` - Data loading
- `EntityDataWriterImpl` - Data writing
- `EntityDbMeta` - DB metadata
- `EntityDatasourceFactoryImpl` - Datasource factory
- `AggregationUtil` - Aggregations

### Entity Condition Classes
- `ConditionAlias` - Alias handling
- `ConditionField` - Field references
- `FieldValueCondition` - Field = value
- `FieldToFieldCondition` - Field = field
- `BasicJoinCondition` - Table joins
- `DateCondition` - Date comparisons
- `ListCondition` - IN lists
- `WhereCondition` - Raw SQL
- `TrueCondition` - Always true

### Elasticsearch Integration
- `ElasticEntityFind` - ES queries
- `ElasticEntityValue` - ES documents
- `ElasticEntityListIterator` - ES results
- `ElasticDatasourceFactory` - ES datasource
- `ElasticSynchronization` - Sync logic

### Screen Implementations
- `ScreenRenderImpl` - Screen rendering
- `ScreenFacadeImpl` - Screen facade
- `ScreenDefinition` - Screen definitions
- `ScreenTree` - Screen tree
- `ScreenForm` - Form handling
- `ScreenWidgets` - Widget rendering
- `ScreenSection` - Sections
- `ScreenUrlInfo` - URL handling
- `ScreenWidgetRender` - Widget render base
- `ScreenWidgetRenderFtl` - FTL rendering
- `ScreenTestImpl` - Screen testing

### Service Implementations
- `ServiceCallSyncImpl` - Sync calls
- `ServiceCallAsyncImpl` - Async calls
- `ServiceCallImpl` - Call implementation
- `ServiceCallSpecialImpl` - Special calls
- `ServiceCallJobImpl` - Job implementation
- `ServiceDefinition` - Service metadata
- `ServiceFacadeImpl` - Service facade
- `ServiceEcaRule` - Service ECAs
- `ScheduledJobRunner` - Job scheduler
- `RestApi` - REST API
- `ServiceJsonRpcDispatcher` - JSON-RPC

### Service Runners
- `InlineServiceRunner` - Inline execution
- `ScriptServiceRunner` - Groovy scripts
- `JavaServiceRunner` - Java classes
- `EntityAutoServiceRunner` - Entity CRUD
- `RemoteJsonRpcServiceRunner` - JSON-RPC remote
- `RemoteRestServiceRunner` - REST remote

### WebApp Implementations
- `MoquiServlet` - Main servlet
- `MoquiContextListener` - Context lifecycle
- `MoquiSessionListener` - Session lifecycle
- `MoquiAuthFilter` - Authentication
- `MoquiAbstractEndpoint` - Endpoint base
- `MoquiFopServlet` - FOP printing
- `NotificationEndpoint` - Notifications
- `NotificationWebSocketListener` - WebSocket
- `GroovyShellEndpoint` - Groovy shell

### Template Renderers
- `FtlTemplateRenderer` - FreeMarker
- `GStringTemplateRenderer` - GString
- `MarkdownTemplateRenderer` - Markdown
- `FtlMarkdownTemplateRenderer` - FTL+Markdown
- `NoTemplateRenderer` - None

### Script Runners
- `GroovyScriptRunner` - Groovy
- `XmlActionsScriptRunner` - XML
- `JavaxScriptRunner` - JavaScript

### Tool Factories
- `MCacheToolFactory` - JCache
- `H2ServerToolFactory` - H2
- `JCSCacheToolFactory` - JCS
- `JackrabbitRunToolFactory` - JCR
- `SubEthaSmtpToolFactory` - SMTP

## Framework Services (145)

### Basic Services
`noop`, `echo#Data`, `convert#Uom`, `create#UomConversion`, `update#UomConversion`, `find#StatusItem`, `find#StatusFlowTransitionToDetail`, `find#Enumeration`, `find#EnumerationByParent`, `get#EnumsByTypeForDropDown`, `get#GeoRegionsForDropDown`

### User Services
`login#UserAccount`, `create#UserAccount`, `update#UserAccount`, `update#Password`, `enable#UserAccount`, `disable#UserAccount`, `reset#Password`, `set#Preference`, `get#ExternalUserAuthcFactorInfo`, `validate#UserAuthcFactorCode`, `create#UserAuthcFactorTotp`, `setup#UserAuthcFactorTotp`, `create#InitialAdminAccount`

### Entity Services
`get#DataFeedDocuments`, `create#DataDocument`, `update#DataDocument`, `export#EntityDataSnapshot`, `import#EntityDataSnapshot`, `receive#DataFeed`, `add#ManualDocumentData`

### Screen Services
`render#ScheduledScreens`, `get#FormResponse`, `create#FormResponse`, `update#FormResponse`, `delete#FormResponse`

### Email Services
`send#Email`, `send#EmailMessage`, `send#EmailTemplate`, `poll#EmailServer`, `process#EmailEca`

### Wiki Services
`get#WikiPageInfoById`, `get#PublishedWikiPageText`, `update#WikiPage`, `create#WikiSpace`, `create#WikiBlog`

### System Message Services
`receive#SystemMessage`, `consume#SystemMessage`, `send#SystemMessage`, `queue#SystemMessage`, `cancel#SystemMessage`

### Search Services
`index#DataDocuments`, `search#DataDocuments`, `delete#DataDocument`, `delete#ElasticIndex`

### Instance Services
`init#Instance`, `start#Instance`, `stop#Instance`, `create#AppInstance`, `init#InstanceDocker`

### Print Services
`get#ServerPrinters`, `print#Document`, `send#PrintJob`, `hold#PrintJob`, `cancel#PrintJob`

## Framework Entities (152)

### Security
`UserAccount`, `UserLoginHistory`, `UserPasswordHistory`, `UserGroup`, `UserGroupMember`, `UserGroupPermission`, `ArtifactAuthz`, `ArtifactHit`, `ArtifactTarpit`

### Status & Enumeration
`StatusItem`, `StatusFlow`, `StatusFlowTransition`, `Enumeration`, `EnumerationType`

### UOM & Geo
`Uom`, `UomConversion`, `Geo`, `GeoAssoc`, `GeoPoint`

### Screen & Form
`DbForm`, `DbFormField`, `FormResponse`, `DynamicScreen`, `SubscreensItem`, `ScreenTheme`

### Wiki & Blog
`WikiSpace`, `WikiPage`, `WikiPageHistory`, `WikiBlog`

### Email & Notification
`EmailServer`, `EmailMessage`, `EmailTemplate`, `NotificationMessage`, `NotificationTopic`

### Service & Data
`ServiceRegister`, `ServiceJob`, `ServiceJobRun`, `DataDocument`, `DataFeed`, `DataSource`

### Message
`SystemMessage`, `SystemMessageType`, `SystemMessageRemote`, `SystemMessageError`

### Resources
`DbResource`, `DbResourceFile`, `LocalizedMessage`

### Test
`Foo`, `Bar`, `TestEntity`, `TestIntPk`

## Framework Tests (18)

- `MoquiSuite` - Test suite
- `EntityCrud` - CRUD operations
- `EntityFindTests` - Query tests
- `EntityNoSqlCrud` - NoSQL CRUD
- `ServiceFacadeTests` - Service tests
- `UserFacadeTests` - User/auth tests
- `MessageFacadeTests` - Message tests
- `CacheFacadeTests` - Cache tests
- `L10nFacadeTests` - Localization tests
- `TransactionFacadeTests` - Transaction tests
- `ResourceFacadeTests` - Resource tests
- `ConcurrentExecution` - Concurrency
- `SystemScreenRenderTests` - Screen rendering
- `ToolsRestApiTests` - REST API
- `TimezoneTest` - Timezone handling
- `SubSelectTests` - Sub-select queries

## Summary

| Category | Count |
|----------|-------|
| Java Interfaces | 42 |
| Java Classes | 38 |
| Groovy Implementation Files | 130 |
| Framework Services | 145 |
| Framework Entities | 152 |
| Test Files | 18 |

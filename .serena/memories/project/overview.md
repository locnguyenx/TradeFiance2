# Project Overview

## moqui-opencode

Moqui Framework with TradeFinance component for Letter of Credit management.

## Project Structure

```
moqui-opencode/
├── framework/          # Moqui Framework Core (Java/Groovy)
├── runtime/
│   ├── base-component/  # Base web tools (110 screens)
│   └── component/       # Application Components
│       ├── TradeFinance/     # LC Management (16 entities, 69 services, 20 tests)
│       ├── mantle-usl/       # Business Services (790+ services)
│       ├── mantle-udm/       # Data Model (422 entities)
│       ├── SimpleScreens/    # UI Screens (367 screens)
│       ├── MarbleERP/        # ERP Dashboard
│       └── moqui-fop/       # PDF Generation
```

## Key Components

| Component | Entities | Services | Screens |
|-----------|----------|----------|---------|
| TradeFinance | 16 | 69 | 23 |
| mantle-usl | View Entities | 790+ | - |
| mantle-udm | 422 | - | - |
| SimpleScreens | - | - | 367 |

## Language Support

- Java (Framework core APIs)
- Groovy (Implementations, Services, Tests)
- XML (Entities, Services, Screens)
- YAML (Configuration)

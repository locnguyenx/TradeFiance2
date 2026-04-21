# Moqui Core Syntax Reference

## 1. Data Preparation & Logic (Inside `<actions>`)
| Tag | Key Attributes | Usage |
| :--- | :--- | :--- |
| `<entity-find>` | `entity-name`, `list` | Fetches data from DB. Use `<search-form-inputs/>` for filtering. |
| `<entity-find-one>` | `entity-name`, `value-field` | Fetches single record by PK. |
| `<service-call>` | `name`, `in-map`, `out-map` | Calls a service. Use `transition#Name` for screen transitions. |
| `<transition-include>` | `name`, `location` | Reuses a transition defined in another XML screen file. Essential for centralized logic. |
| `<parameter>` | `name`, `required="true"` | Defines screen parameters. Marking primary IDs as `required="true"` in detail sub-screens is a best practice for clean tabbed navigation. |
| `<set>` | `field`, `from`, `value` | Sets a variable. Use Groovy expressions in `from`. |
| `<script>` | `type="groovy"` | Multi-line logic. Wrap in `<![CDATA[ ... ]]>`. |
| `<if>` / `<else>` | `condition` | Conditional logic. `<else>` MUST be nested inside `<if>`. |
| `<return>` | `message`, `error` | Exit action block early. |
15: | `<entity-find-one>` | `entity-name`, `value-field` | **Refresh Pattern:** Use after status transition services to refresh the local context from DB. |

## 2. UI Structure & Containers
| Tag | Key Attributes | Usage |
| :--- | :--- | :--- |
| `<screen>` | `require-authentication, default-menu-include, default-menu-title, default-menu-index` | Root element. Use `default-menu-include="false"` for search screens to keep the navigation clean. |
| `<subscreens>` | `default-item` | Defines child screens/tabs. |
| `<subscreens-panel>` | `type="tab\|popup"` | Renders subscreen navigation. |
| `<widgets>` | N/A | Container for visual elements. |
| `<container>` | `style`, `type` | Generic div. Use `style="q-card"` for Quasar cards. |
| `<container-row>` | N/A | Grid row. |
| `<section>` | `name`, `condition` | Conditionally rendered area. |
| `<render-mode>` | `text`, `html` | Raw HTML/Text output. Wrap in `<![CDATA[ ... ]]>`. |

## 3. Forms & Interaction
| Tag | Key Attributes | Usage |
| :--- | :--- | :--- |
| `<form-list>` | `name`, `list`, `transition` | Data table (Quasar q-table). Use `header-dialog="true"`. |
| `<form-single>` | `name`, `transition` | Detail/Edit form. Use `reload-save="true"`. |
| `<field>` | `name`, `title` | Field definition. Must contain a widget. |
| `<field-layout>` | N/A | Precise field positioning. Use `<field-ref>`. |
| `<field-ref>` | `name` | References a defined field in layout. |

## 4. Field Widgets (Inside `<field>`)
| Tag | Key Attributes | Usage |
| :--- | :--- | :--- |
| `<default-field>` | N/A | Standard display wrapper. |
| `<text-line>` | `size`, `ac-transition` | Single line input. |
| `<text-area>` | `rows`, `cols` | Multi-line input. |
| `<drop-down>` | `allow-empty`, `required` | Select menu. Use `<entity-options>` or `<option>`. |
| `<date-time>` | `type="date\|time"` | Date/Time picker. |
| `<display>` | `format`, `currency` | Read-only value. |
| `<link>` | `url`, `text`, `url-type` | Anchor. Use `url-type="screen"`. |
| `<submit>` | `text`, `type` | Action button. |

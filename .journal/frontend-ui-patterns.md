# Frontend UI Patterns (React / Next.js)

> Patterns for the Blue Premium enterprise frontend.

## 1. Layout & Styling Patterns

### Scoped CSS (styled-jsx) Pitfalls
Next.js `styled-jsx` adds unique class attributes (e.g., `jsx-xxxx`) to scope CSS. However, this scoping can sometimes fail for:
- **Pseudo-elements**: `::-webkit-scrollbar` may not correctly inherit scoped parent selectors.
- **Deep Nesting**: Complex grid/flex layouts might not resolve properties like `overflow` or `min-height` correctly in computed styles if applied via scoped CSS.
- **Solution**: Use **inline styles** for layout-critical properties (`overflow`, `flex`, `min-height`, `height`) on the exact elements that require them. This bypasses scoping issues and ensures the browser applies the styles as expected.

### Flexible Height Management
To allow internal components (like master-detail views) to manage their own scrolling:
1. **Constrain Parent**: Ensure the main application wrapper (`.main-wrapper`) has a fixed height (e.g., `100vh`) and `overflow: hidden`.
2. **Content Container**: The immediate content area (`.content`) should have `flex: 1`, `overflow-y: auto`, and `min-height: 0` to allow children to shrink/grow.
3. **Flexible Children**: Child components should use `height: 100%` and `min-height: 0` instead of `calc(100vh - PX)` to fill available space dynamically.

## 2. Component Design

### Master-Detail Views
- Use a `display: grid` layout for the main container.
- Panes should have `display: flex`, `flex-direction: column`, and `overflow: hidden`.
- The actual scrollable content inside a pane should have `flex: 1` and `overflow-y: auto`.
- Always use `min-height: 0` on flex/grid children that are expected to scroll internally to prevent them from expanding to fit their content.

# Save Workflow Command

## Description
Saves current workflow phase output to file.

## Usage
```
/save-workflow [phase] [content]
```

## Parameters
- `phase`: Phase name (e.g., phase1-analysis, phase2-discovery, phase3-design, phase4-implementation, phase5-validation)
- `content`: Content to save (can be multi-line)

## Actions
1. Creates `.opencode/workflow/` directory if not exists
2. Saves content to `.opencode/workflow/[phase].md`
3. Updates `.opencode/workflow/state.md`

## Examples
```
> /save-workflow phase1-analysis "Analysis of Import LC credit limit validation..."
✓ Saved to .opencode/workflow/phase1-analysis.md

> /save-workflow phase3-design "Entity XML: <entity>..."
✓ Saved to .opencode/workflow/phase3-design.md
```

## File Format
Each phase file includes:
- Phase title
- Content
- Timestamp
- References to previous phases

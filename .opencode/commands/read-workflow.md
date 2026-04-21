# Read Workflow Command

## Description
Reads workflow phase output from file.

## Usage
```
/read-workflow [phase]
```

## Parameters
- `phase`: Phase name (e.g., phase1-analysis, phase2-discovery, phase3-design, phase4-implementation, phase5-validation)

## Actions
1. Reads `.opencode/workflow/[phase].md`
2. Displays content

## Examples
```
> /read-workflow phase1-analysis
[Displays content of phase1-analysis.md]

> /read-workflow phase3-design
[Displays content of phase3-design.md]
```

## Available Phases
- phase1-analysis
- phase2-discovery
- phase3-design
- phase4-implementation
- phase5-validation

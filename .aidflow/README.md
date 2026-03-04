# aidflow

Session-based development workflow manager for Claude Code.

## Directory Structure

```
.aidflow/
  config.yaml     # Configuration
  sessions/       # Active sessions
    {name}/
      meta.json   # Session metadata
      plan.md     # Work plan (optional)
  history/        # Archived sessions
    YYMMDD_{name}/
      meta.json
      plan.md
  guides/         # Project-specific guides
    *.md
  worktrees/      # Git worktrees (gitignored)
```

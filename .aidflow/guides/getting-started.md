# Getting Started
Quick guide to using aidflow in this project.

## Session Workflow

1. **Start**: `session create "task-name"` to begin a new task
2. **Plan** (optional): `plan create` for medium/large tasks
3. **Work**: Implement using Claude Code's native tools
4. **Check**: `session status` to see progress
5. **Done**: `session complete` to archive

## Worktrees

For isolated parallel work, create sessions with worktrees:
- Set `worktree: true` in `session create`
- Or set `worktree.auto: true` in config.yaml for all sessions

## Guides

Add project-specific guides to `.aidflow/guides/` as markdown files.
Use `guide list` to see available guides.

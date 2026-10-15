# Commitlint Action

This action lints commit messages using Commitlint to enforce the Conventional Commits standard.

## Features

- Lints commit messages against Conventional Commits standard
- Works on both push and pull_request events
- Fast failure detection when placed early in workflows
- Reusable across all workflows
- Configurable config file path
- Option to fail on warnings

## Usage

Place this action early in your workflow (after checkout) for fastest feedback:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Required for commitlint to compare commits
      - uses: .github/actions/commitlint
        with:
          config-file: './commitlint.config.js'  # Optional, defaults to this
          fail-on-warning: 'false'  # Optional, defaults to false
      # Continue with other steps...
```

## Inputs

- `config-file`: Path to commitlint config file (default: './commitlint.config.js')
- `fail-on-warning`: Whether to fail on warnings (default: 'false')

## How it works

1. Ensures code is checked out with full history (`fetch-depth: 0`)
2. Sets up Node.js with npm caching
3. Installs commitlint dependencies
4. Runs commitlint to check commits since the base branch
5. Fails the job if any commit messages don't conform to Conventional Commits

## Required setup

This action expects:
- A `commitlint.config.js` file in your repository (or specify custom path)
- Conventional Commits format for commit messages:
  - `type(scope): description`
  - Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
  - Optional scope in parentheses
  - Required colon and space after type/scope
  - Description summarizing the changes

## Example commitlint.config.js

```javascript
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'perf',
        'test',
        'build',
        'ci',
        'chore',
        'revert'
      ]
    ],
    'subject-empty': [2, 'never'],
    'type-empty': [2, 'never'],
    'scope-empty': [2, 'never'],
    'header-max-length': [2, 'always', 100]
  }
};
```

## Example workflow placement

For fastest failure detection, place this immediately after checkout:

```yaml
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: .github/actions/commitlint
        # Will fail fast if commit messages are bad
      - uses: .github/actions/setup-java-gradle
        # Only runs if commitlint passes
        with:
          java-version: '21'
          distribution: 'temurin'
          gradle-cache: 'true'
      # ... rest of workflow
```

This ensures you get immediate feedback on commit message issues before wasting time on expensive operations like building or testing.
---
name: github-ssh-setup
description: Use when creating an SSH key for GitHub, verifying GitHub SSH authentication, configuring git identity and commit signing with SSH, or setting the default GitHub remote URL.
---

# GitHub SSH Setup

## Overview

Reproducible method for creating an Ed25519 SSH key, configuring git to use it for identity and signed commits, and verifying GitHub authentication works.

## When to Use

- Fresh workspace needs GitHub SSH access
- Need to print a public key for GitHub
- Need to confirm `ssh -T git@github.com` succeeds
- Configuring git identity and SSH commit signing together
- Resetting default GitHub remote to `rahaaatul/Hail`

Do NOT use for non-GitHub hosts or non-SSH auth methods.

## Quick Reference

| Step | Command / Action |
|------|------------------|
| Create key | `ssh-keygen -t ed25519 -C "<comment>" -f ~/.ssh/id_ed25519 -N ""` |
| Start agent | `eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519` |
| Git identity | `git config --global user.name "<name>"` and `user.email "<email>"` |
| Commit signing | `git config --global gpg.format ssh`, `user.signingkey "<key content>"`, `commit.gpgsign true` |
| Remote | `git remote set-url origin git@github.com:rahaaatul/Hail.git` |
| Verify agent | `ssh-add -l` |
| Verify config | `git config user.signingkey`, `git config commit.gpgsign` |
| Verify GitHub | `ssh -T git@github.com` |

## Implementation

### 1. Create key

```bash
ssh-keygen -t ed25519 -C "hail@workspace" -f ~/.ssh/id_ed25519 -N ""
```

Use `~/.ssh/id_ed25519` as the file path. Use a stable comment, not an email, to avoid leaking personal data in logs.

### 2. Load into ssh-agent

```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
```

Verify with `ssh-add -l`. If it says "The agent has no identities", the key is not loaded.

### 3. Configure git identity

```bash
git config --global user.name "<name>"
git config --global user.email "<email>"
```

### 4. Configure SSH commit signing

```bash
git config --global gpg.format ssh
git config --global user.signingkey "<public key content>"
git config --global commit.gpgsign true
```

`user.signingkey` must be the **public key content** (e.g. `ssh-ed25519 AAAAC3...`), NOT the file path.

### 5. Set default remote

```bash
git remote set-url origin git@github.com:rahaaatul/Hail.git
```

Use exact owner/repo casing. Confirm with `git remote -v`.

### 6. Verify

```bash
git config user.signingkey
git config commit.gpgsign
ssh-add -l
ssh -T git@github.com
```

Expected GitHub response: `Hi <username>! You've successfully authenticated, but GitHub does not provide shell access.`

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Using non-standard key path (`github_ed25519`) | Use `~/.ssh/id_ed25519` |
| Setting `user.signingkey` to a file path | Set it to the public key content string |
| Forgetting to start ssh-agent or add key | Run `eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519` |
| Verifying GitHub before key is added to account | `ssh -T git@github.com` returns `Permission denied (publickey)` until the public key is added on GitHub |

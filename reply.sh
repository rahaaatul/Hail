#!/usr/bin/env bash
# usage: reply.sh <threadId> <bodyFile>
# Replies to a PR review thread and resolves it.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
body="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
cd "$here/worktree-pr109"
gh api graphql \
  -f query='mutation($id:ID!,$body:String!){addPullRequestReviewThreadReply(input:{pullRequestReviewThreadId:$id,body:$body}){comment{url}}}' \
  -f id="$1" -F body=@"$body" --jq '.data.addPullRequestReviewThreadReply.comment.url'
gh api graphql \
  -f query='mutation($id:ID!){resolveReviewThread(input:{threadId:$id}){thread{isResolved}}}' \
  -f id="$1" --jq '.data.resolveReviewThread.thread.isResolved'

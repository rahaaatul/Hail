// The release configuration.
//
// The release notes describe the changes to the app and nothing else. A
// commit is included in the notes when it touched the app, so the work on
// the build and on the workflows stays out of the release notes, and the
// notes say what a person who installs the app would notice.
//
// The version name and the version code are set by
// .github/scripts/prepare-release.sh, which also holds the rules for how a
// prerelease is numbered.

import { execFileSync } from 'node:child_process'
import conventionalChangelogConventionalCommits from 'conventional-changelog-conventionalcommits'

// The paths that make a commit a change to the app.
const APP_PATHS = ['app/', 'gradle/libs.versions.toml']

// Whether the commit touched the app. A commit whose files cannot be read
// is treated as an app change, so nothing is lost by accident.
function touchesApp(hash) {
  try {
    const files = execFileSync('git', ['show', '--name-only', '--pretty=format:', hash], {
      encoding: 'utf8'
    })
    return files
      .split('\n')
      .some((file) => APP_PATHS.some((path) => file === path || file.startsWith(path)))
  } catch {
    return true
  }
}

const preset = conventionalChangelogConventionalCommits({
  types: [
    // The work on the build and on the workflows is left out of the notes.
    // A rule with a scope is matched before a rule without one, so these
    // come first.
    { type: 'feat', scope: 'build-apk', hidden: true },
    { type: 'feat', scope: 'commitlint', hidden: true },
    { type: 'feat', scope: 'release', hidden: true },
    { type: 'fix', scope: 'actions', hidden: true },
    { type: 'fix', scope: 'build', hidden: true },
    { type: 'fix', scope: 'ci', hidden: true },
    { type: 'fix', scope: 'codeql', hidden: true },
    { type: 'fix', scope: 'release', hidden: true },
    { type: 'build', section: '🔧 Build System', hidden: true },
    { type: 'chore', section: '🔧 Miscellaneous Chores', hidden: true },
    { type: 'ci', section: '🔧 Continuous Integration', hidden: true },
    { type: 'docs', section: '🔧 Documentation', hidden: true },
    { type: 'merge', section: '🔧 Merges', hidden: true },
    { type: 'refactor', section: '🔧 Code Refactoring', hidden: true },
    { type: 'style', section: '🔧 Styles', hidden: true },
    { type: 'test', section: '🔧 Tests', hidden: true },
    { type: 'feat', section: '✨ New Features', hidden: false },
    { type: 'fix', section: '🐛 Bug Fixes', hidden: false },
    { type: 'perf', section: '🔧 Improvements', hidden: false },
    { type: 'revert', section: '⏪ Reverts', hidden: false }
  ]
})

export default {
  branches: ['main', { name: 'dev', prerelease: true }],
  tagFormat: 'v${version.replace("-dev.", ".")}',
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        releaseRules: [
          { type: 'fix', release: 'patch' },
          { type: 'feat', release: 'minor' },
          { type: 'perf', release: 'patch' },
          { type: 'build', scope: 'Needs bump', release: 'patch' }
        ]
      }
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        parserOpts: preset.parser,
        writerOpts: {
          ...preset.writer,
          transform: (commit, context) =>
            touchesApp(commit.hash) ? preset.writer.transform(commit, context) : false
        }
      }
    ],
    [
      '@semantic-release/exec',
      {
        prepareCmd: 'bash .github/scripts/prepare-release.sh ${nextRelease.version}'
      }
    ],
    [
      '@semantic-release/git',
      {
        assets: ['gradle.properties', 'versionCode.properties', 'CHANGELOG.md'],
        message: 'chore(release): ${nextRelease.version} [skip ci]'
      }
    ],
    [
      '@semantic-release/github',
      {
        assets: [{ path: 'app/build/outputs/apk/**/*.apk' }],
        successComment: false
      }
    ],
    [
      '@cleyrop-org/semantic-release-backmerge',
      {
        backmergeBranches: [{ from: 'main', to: 'dev' }],
        clearWorkspace: true,
        ignorePaths: ['gradle.properties']
      }
    ]
  ]
}

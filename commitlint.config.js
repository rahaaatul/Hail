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
    'scope-empty': [1, 'never'],
    'header-max-length': [2, 'always', 100]
  },
  ignores: [
    (commit) => {
      const subject = commit.subject || '';
      return !/^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)[(:]/.test(subject);
    },
  ],
};
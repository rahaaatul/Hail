import { defineConfig } from 'vitepress'
import { resolve } from 'path'

export default defineConfig({
  title: 'Hail',
  description: 'Freeze Android apps',
  base: '/Hail/',
  lastUpdated: true,
  cleanUrls: true,
  head: [
    ['link', { rel: 'icon', href: '/favicon.svg' }]
  ],
  vite: {
    resolve: {
      alias: {
        '/screenshots': resolve(__dirname, '../.vitepress/public/screenshots')
      }
    }
  },
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/what-is-hail' },
      { text: 'App Management', link: '/guide/app-management' },
      { text: 'Actions', link: '/guide/actions' },
      { text: 'Backup & Restore', link: '/guide/backup-restore' },
      { text: 'Working Mode', link: '/guide/working-mode' },
      { text: 'API', link: '/guide/api' },
      { text: "What's New", link: '/guide/changelog' }
    ],
    sidebar: [
      {
        text: 'Getting Started',
        items: [
          { text: 'What is Hail', link: '/guide/what-is-hail' },
          { text: 'App Management', link: '/guide/app-management' },
          { text: 'Freeze', link: '/guide/freeze' },
          { text: 'Working Mode', link: '/guide/working-mode' },
          { text: 'Revert', link: '/guide/revert' }
        ]
      },
      {
        text: 'Features',
        items: [
          { text: 'Actions', link: '/guide/actions' },
          { text: 'Backup & Restore', link: '/guide/backup-restore' }
        ]
      },
      {
        text: 'Reference',
        items: [
          { text: 'API', link: '/guide/api' },
          { text: "What's New", link: '/guide/changelog' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/rahaaatul/Hail' }
    ],
    footer: {
      message: 'Released under the GNU General Public License v3.0.',
      copyright: 'Copyright (C) 2021-2026 Aistra'
    },
    editLink: {
      pattern: 'https://github.com/rahaaatul/Hail/edit/docs/:path'
    },
    outline: {
      label: 'On this page'
    },
    docFooter: {
      prev: 'Previous page',
      next: 'Next page'
    },
    lastUpdated: {
      text: 'Updated at'
    },
    search: {
      provider: 'local'
    },
    logo: '/logo.svg'
  }
})
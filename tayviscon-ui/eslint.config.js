import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import eslintConfigPrettier from 'eslint-config-prettier/flat'
import globals from 'globals'

export default [
  { ignores: ['dist/**', 'node_modules/**', 'scripts/**'] },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: { ...globals.browser },
    },
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },
  {
    files: ['vite.config.js'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  eslintConfigPrettier,
]

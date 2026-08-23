/// <reference types='vitest' />
import path from 'path';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';
import customHtmlPlugin from './vite-plugins/html-plugin';

// The original builds this app out of a monorepo, resolving four sibling packages through
// workspace aliases. Here those sources sit under vendor/ and the aliases point at them, so
// the app builds on its own. Everything else — plugins, dedupe list, output shape — matches
// packages/web/vite.config.mts in the original.
export default defineConfig(() => {
  const AP_TITLE = 'Activepieces';
  const AP_FAVICON = 'https://activepieces.com/favicon.ico';

  return {
    root: __dirname,
    resolve: {
      dedupe: [
        '@codemirror/state',
        '@codemirror/view',
        '@codemirror/language',
        '@codemirror/commands',
      ],
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@activepieces/shared': path.resolve(__dirname, './vendor/core/shared/src'),
        'ee-embed-sdk': path.resolve(__dirname, './vendor/ee/embed-sdk/src'),
        '@activepieces/pieces-framework': path.resolve(__dirname, './vendor/pieces/framework/src'),
        '@activepieces/core-utils': path.resolve(__dirname, './vendor/core/utils/src'),
        '@activepieces/core-formula': path.resolve(__dirname, './vendor/core/formula/src'),
        '@activepieces/core-piece-types': path.resolve(__dirname, './vendor/core/piece-types/src'),
        '@activepieces/core-execution': path.resolve(__dirname, './vendor/core/execution/src'),
      },
    },
    plugins: [
      react(),
      tailwindcss(),
      customHtmlPlugin({ title: AP_TITLE, icon: AP_FAVICON }),
    ],
    build: {
      outDir: '../src/main/resources/static-resources',
      emptyOutDir: true,
      reportCompressedSize: false,
      sourcemap: false,
      commonjsOptions: { transformMixedEsModules: true },
    },
  };
});

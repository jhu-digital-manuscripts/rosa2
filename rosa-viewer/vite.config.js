import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { existsSync, readdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * The viewer is built in three ways:
 *
 * - `vite` (dev): serves sites/ with a chooser page at / and the AoR and DLMM
 *   sites at /aor/ and /dlmm/, proxying IIIF and Opensearch to local-dev.
 * - `vite build`: library build of src/index.js (Mirador and React external).
 * - `ROSA_SITE=<aor|dlmm> vite build` (via scripts/build-sites.mjs): a
 *   self-contained static site in dist/sites/<site>/ that bundles everything.
 *
 * Deployment-specific values come from VITE_ROSA_* variables (.env, .env.local
 * or the environment) and are substituted into sites/<site>/index.html.
 */

const viewerDir = dirname(fileURLToPath(import.meta.url));

/** Site ids: every sites/<id>/ directory that has an index.html. */
export function listSites(dir = resolve(viewerDir, 'sites')) {
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && existsSync(resolve(dir, entry.name, 'index.html')))
    .map((entry) => entry.name)
    .sort();
}

const common = {
  plugins: [react()],
  envDir: viewerDir,
  publicDir: resolve(viewerDir, 'public'),
};

const server = {
  port: 3001,
  fs: { allow: [viewerDir] },
  proxy: {
    // IIIF Presentation API files served by local-dev
    '/iiif': {
      target: 'http://localhost:3000',
      changeOrigin: true,
    },
    // Opensearch endpoints - proxy to local Opensearch instance.
    // The jhsearch.json specifies /_search, and the opensearch service
    // may rewrite it to /manifest/_search or /canvas/_search
    '/manifest/_search': {
      target: 'http://localhost:9200',
      changeOrigin: true,
    },
    '/canvas/_search': {
      target: 'http://localhost:9200',
      changeOrigin: true,
    },
    '/_search': {
      target: 'http://localhost:9200',
      changeOrigin: true,
    },
  },
};

function libraryConfig() {
  return {
    ...common,
    root: viewerDir,
    build: {
      outDir: resolve(viewerDir, 'dist'),
      sourcemap: true,
      lib: {
        entry: resolve(viewerDir, 'src/index.js'),
        name: 'RosaViewer',
        fileName: (format) => `rosa-viewer.${format}.js`,
        formats: ['es', 'umd'],
      },
      rollupOptions: {
        external: ['react', 'react-dom', 'react-redux', 'mirador'],
        output: {
          globals: {
            react: 'React',
            'react-dom': 'ReactDOM',
            'react-redux': 'ReactRedux',
            mirador: 'Mirador',
          },
        },
      },
    },
  };
}

function siteConfig(site) {
  const sites = listSites();
  if (!sites.includes(site)) {
    throw new Error(`Unknown ROSA_SITE "${site}". Expected one of: ${sites.join(', ')}`);
  }
  return {
    ...common,
    root: resolve(viewerDir, 'sites', site),
    // Relative asset URLs so the built site works from any path (e.g. /viewer/
    // on a WordPress host or /rosa2/aor/ on GitHub Pages). Override with
    // ROSA_BASE_PATH when an absolute base is preferred.
    base: process.env.ROSA_BASE_PATH || './',
    appType: 'mpa',
    build: {
      outDir: resolve(viewerDir, 'dist/sites', site),
      emptyOutDir: true,
      sourcemap: false,
      // Mirador is bundled as one chunk; the default 500 kB warning is noise here.
      chunkSizeWarningLimit: 2500,
    },
  };
}

function devConfig() {
  return {
    ...common,
    root: resolve(viewerDir, 'sites'),
    appType: 'mpa',
    server,
  };
}

export default defineConfig(({ command }) => {
  if (command === 'build') {
    return process.env.ROSA_SITE ? siteConfig(process.env.ROSA_SITE) : libraryConfig();
  }
  return devConfig();
});

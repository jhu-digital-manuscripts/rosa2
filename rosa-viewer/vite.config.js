import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  root: '.',
  build: {
    outDir: 'dist',
    sourcemap: true,
    lib: {
      entry: resolve(__dirname, 'src/index.js'),
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
  server: {
    port: 3001,
    proxy: {
      // IIIF Presentation API files served by local-dev
      '/iiif': {
        target: 'http://localhost:3000',
        changeOrigin: true,
      },
      // Logos served from rosa-viewer/public/logo/ (no proxy needed, served by vite)
      // But if logos were served from the IIIF server:
      // '/logo': {
      //   target: 'http://localhost:3000',
      //   changeOrigin: true,
      // },
      
      // Opensearch endpoints - proxy to local Opensearch instance
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
  },
});

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
      '/iiif': {
        target: 'http://localhost:3000',
        changeOrigin: true,
      },
      '/logo': {
        target: 'http://localhost:3000',
        changeOrigin: true,
      },
    },
  },
});

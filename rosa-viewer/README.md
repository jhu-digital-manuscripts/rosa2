# Rosa Viewer

A Mirador 4-based IIIF viewer for browsing digitized book collections with integrated Opensearch-powered search and faceted browsing.

## Overview

Rosa Viewer provides:
- Collection browsing with faceted filtering
- Full-text search across manifest and canvas metadata
- Advanced search with per-field queries and boolean operators
- Integration with Mirador's page-turner viewer

## Prerequisites

- Node.js 18+
- npm 9+

## Installation

```bash
npm install
```

## Development

Start the development server:

```bash
npm run dev
```

The viewer will be available at `http://localhost:3001`.

For development, you'll need:
- IIIF Presentation API server running (default: `http://localhost:3000/iiif`)
- Opensearch service with manifest and canvas indexes

## Build

Build for production:

```bash
npm run build
```

Output is generated in the `dist/` directory.

## Testing

Run unit tests:

```bash
npm test
```

Run tests with coverage:

```bash
npm run test:coverage
```

## Configuration

Configure the viewer through Mirador's initialization:

```javascript
import Mirador from 'mirador';
import { jhsearchPlugin } from './plugins/jhsearch';

Mirador.viewer({
  id: 'rosa-viewer',
  jhsearch: {
    // Required: Base URL for IIIF Presentation API
    iiifBaseUrl: 'https://iiif.example.org',
    
    // Required: Top-level collection ID
    collectionId: 'rose',
    
    // Optional: Child collection IDs
    childCollectionIds: [],
    
    // Optional: IIIF Image API base URL
    imageBaseUrl: 'https://image.example.org/iiif',
    
    // Optional: Thumbnail URL template
    thumbnailTemplate: '{imageBaseUrl}/{iiifImageId}/full/{width},/0/default.jpg',
    
    // Optional: Thumbnail width (default: 80)
    thumbnailWidth: 80,
    
    // Optional: Logo base URL (default: '/logo')
    logoBaseUrl: '/logo',
    
    // Optional: Enable faceted browsing (default: true)
    enableFacets: true,
    
    // Optional: Default results per page (default: 20)
    pageSize: 20,
  }
}, [
  ...jhsearchPlugin,
]);
```

### Configuration Options

| Option | Type | Required | Default | Description |
|--------|------|----------|---------|-------------|
| `iiifBaseUrl` | string | Yes | - | Base URL for IIIF Presentation API |
| `collectionId` | string | Yes | - | Top-level collection identifier |
| `childCollectionIds` | string[] | No | `[]` | Child collection identifiers |
| `imageBaseUrl` | string | No | - | IIIF Image API base URL |
| `thumbnailTemplate` | string | No | (see above) | URL template for thumbnails |
| `thumbnailWidth` | number | No | `80` | Thumbnail width in pixels |
| `logoBaseUrl` | string | No | `'/logo'` | Base URL for collection logos |
| `enableFacets` | boolean | No | `true` | Enable faceted browsing |
| `pageSize` | number | No | `20` | Results per page |
| `pageSizeOptions` | number[] | No | `[20,30,40,50]` | Available page size options |

## Project Structure

```
rosa-viewer/
├── src/
│   ├── index.js              # Entry point
│   ├── config.js             # Default configuration
│   └── plugins/
│       └── jhsearch/         # JHSearch plugin
│           ├── index.js
│           ├── state/        # Redux state management
│           ├── services/     # API clients
│           ├── components/   # React components
│           ├── hooks/        # Custom hooks
│           └── utils/        # Utilities
├── public/
│   └── index.html
├── __tests__/                # Test files
├── package.json
├── vite.config.js
├── vitest.config.js
└── DESIGN.md                 # Detailed design document
```

## Deployment

1. Build the viewer: `npm run build`
2. Deploy `dist/` to your static file server
3. Ensure the following are accessible:
   - IIIF Presentation API (collection.json, manifest.json files)
   - Opensearch `_search` endpoint
   - IIIF Image API for thumbnails
   - Logo images at the configured `logoBaseUrl`

## Dependencies

- [Mirador](https://github.com/ProjectMirador/mirador) - IIIF viewer
- [React](https://reactjs.org/) - UI framework
- [MUI](https://mui.com/) - Component library
- [Vite](https://vitejs.dev/) - Build tool
- [Vitest](https://vitest.dev/) - Testing framework

## Documentation

- [Design Document](./DESIGN.md) - Detailed architecture and specifications
- [Search Documentation](../doc/search.md) - JHSearch service specification
- [Opensearch Indexes](../doc/opensearch-indexes.md) - Index field definitions

## License

See the LICENSE file in the repository root.

# Rosa Viewer

A Mirador 4-based IIIF viewer for browsing digitized book collections with integrated Opensearch-powered search and faceted browsing.

## Overview

Rosa Viewer provides:
- Collection browsing with faceted filtering
- Full-text search across manifest and canvas metadata
- Advanced search with per-field queries and boolean operators
- Integration with Mirador's page-turner viewer

## Prerequisites

- Node.js 20+
- npm 9+

## Installation

```bash
npm install
```

## Sites

The viewer is published as two websites that share the same code but differ in
collection, features and styling:

| Site | Directory | Collection | Facets |
|------|-----------|------------|--------|
| The Archaeology of Reading (AoR) | `sites/aor/` | `aor` | off |
| Digital Library of Medieval Manuscripts (DLMM) | `sites/dlmm/` | `dlmm` with children `rose`, `pizan` | on |

Each site directory holds:

- `index.html` – the page: branded header, the `#rosa-viewer` container and the
  `window.rosaSiteEnv` block with deployment-specific URLs
- `main.js` – the site definition passed to `createSiteViewer()`: collection ids,
  `enableFacets`, MUI theme and header banner. These are fixed for the site and
  cannot be changed by environment variables.
- `site.css` – colour variables layered over the shared `src/site/site.css`

`sites/index.html` is a small chooser page linking to both sites; it is the root
page in local development and on the stage (GitHub Pages) deployment.

### Environment-specific configuration

Values that differ between local-dev, stage and prod are `VITE_ROSA_*` variables,
substituted into each site's `index.html` at build time:

| Variable | Purpose | Default (`.env`) |
|----------|---------|------------------|
| `VITE_ROSA_IIIF_BASE_URL` | IIIF Presentation API base URL. Must match the `--base-url` given to `rosa-tool generate-iiif-pres`. | `http://localhost:3000/iiif` |
| `VITE_ROSA_IMAGE_BASE_URL` | IIIF Image API base URL (thumbnails, header banner) | `https://image.library.jhu.edu/iiif` |
| `ROSA_BASE_PATH` | Vite `base` for site builds. Relative (`./`) by default so a built site works from any path. | `./` |

`.env` is committed and holds the local-dev defaults. Override per machine in
`.env.local` (git-ignored) or per build with environment variables. The values
end up in plain text in the built `index.html`, so they can also be edited there
if a site is moved after it was built.

## Development

Start the local-dev backend (`local-dev/start.sh`, see `../docs/deployment.md`),
then start the development server:

```bash
npm run dev
```

Open `http://localhost:3001` and choose a site, or go directly to
`http://localhost:3001/aor/` or `http://localhost:3001/dlmm/`. The dev server
proxies `/iiif` to the local IIIF file server on port 3000 and the Opensearch
`_search` endpoints to port 9200.

## Build

Build the sites (self-contained static files, Mirador and React bundled):

```bash
npm run build:sites            # both sites -> dist/sites/{aor,dlmm}/ + dist/sites/index.html
npm run build:sites -- aor     # one site
VITE_ROSA_IIIF_BASE_URL=https://example.org/viewer/iiif npm run build:sites -- dlmm
```

Build the viewer as a library (Mirador and React external) for embedding
elsewhere:

```bash
npm run build                  # -> dist/rosa-viewer.{es,umd}.js
```

To produce a complete deployable site including the IIIF Presentation files,
use `scripts/package-site.sh` (see [Deployment](#deployment)).

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
├── .env                      # Committed local-dev defaults for VITE_ROSA_* variables
├── sites/
│   ├── index.html            # Chooser page (root in dev and on stage)
│   ├── aor/                  # AoR site: index.html, main.js, site.css
│   └── dlmm/                 # DLMM site: index.html, main.js, site.css
├── scripts/
│   ├── build-sites.mjs       # npm run build:sites
│   └── package-site.sh       # Viewer + IIIF files -> deployable directory / tarball
├── public/
│   └── logo/                 # Collection logos, copied next to each built site
├── src/
│   ├── index.js              # Library entry point (createViewer)
│   ├── config.js             # Default configuration
│   ├── site/                 # createSiteViewer, site config resolution, shared site CSS
│   ├── i18n/                 # Translations
│   ├── styles/
│   ├── test/                 # Vitest setup and shared mocks
│   └── plugins/
│       └── jhsearch/         # JHSearch plugin
│           ├── index.jsx
│           ├── state/        # Redux state management
│           ├── services/     # API clients
│           ├── components/   # React components
│           ├── hooks/        # Custom hooks
│           └── utils/        # Utilities
├── package.json
├── vite.config.js            # dev (sites/), library build, and per-site builds
└── vitest.config.js          # Tests are colocated as src/**/*.test.{js,jsx}
```

## Deployment

A site is deployed as plain static files: the built viewer plus the IIIF
Presentation files generated by `rosa-tool`. `scripts/package-site.sh` produces
both for one site, with all URLs set for the location it will be served from:

```bash
# from the repository root, after building rosa-tool and running npm ci here
rosa-viewer/scripts/package-site.sh \
  --site aor \
  --site-url https://archaeologyofreading.org/viewer \
  --opensearch-url https://<api-id>.execute-api.us-east-1.amazonaws.com/_search \
  --tarball aor-site.tar.gz
```

Copy the contents of the output directory (or unpack the tarball) at the site
URL so that `<site-url>/index.html` and `<site-url>/iiif/` resolve. The
Opensearch endpoint must allow the site's origin in its CORS configuration.

The GitHub Actions workflows automate this: `Deploy Stage` publishes both sites
to GitHub Pages and `Release` attaches `aor-site.tar.gz` and `dlmm-site.tar.gz`
to a release. See [`docs/deployment.md`](../docs/deployment.md).

## Dependencies

- [Mirador](https://github.com/ProjectMirador/mirador) - IIIF viewer
- [React](https://reactjs.org/) - UI framework
- [MUI](https://mui.com/) - Component library
- [Vite](https://vitejs.dev/) - Build tool
- [Vitest](https://vitest.dev/) - Testing framework

## Documentation

- [Search Documentation](../docs/search.md) - JHSearch service specification
- [Opensearch Indexes](../docs/opensearch-indexes.md) - Index field definitions

## License

See the LICENSE file in the repository root.

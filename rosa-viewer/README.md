# Rosa Viewer

A Mirador 4-based IIIF viewer for browsing digitized book collections with integrated Opensearch-powered search and faceted browsing.

## Overview

Rosa Viewer provides:
- Collection browsing with faceted filtering, including a Name filter that picks
  books by type-ahead over their labels (sites with `enableFacets`, both AoR and DLMM)
- Full-text search across manifest and canvas metadata
- Advanced search with per-field queries and boolean operators
- Integration with Mirador's page-turner viewer
- An annotations panel that groups a page's annotations (AoR by place on the
  page, DLMM by kind) and styles them per collection
- A book description panel for books whose manifest links a TEI description

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
| The Archaeology of Reading (AoR) | `sites/aor/` | `aor` | on, Name filter only (`jhsearch.json` lists no categories) |
| Digital Library of Medieval Manuscripts (DLMM) | `sites/dlmm/` | `dlmm` with children `rose`, `pizan` | on |

Each site directory holds:

- `index.html` – the page: branded header, the `#rosa-viewer` container and the
  `window.rosaSiteEnv` block with deployment-specific URLs
- `main.js` – the site definition passed to `createSiteViewer()`: collection ids,
  collection selector labels, `enableFacets`, MUI theme and header banner. These are fixed for the site and
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

### Annotations

The `RosaAnnotations` plugin (`src/plugins/annotations/`) replaces the list of
annotations Mirador shows for each canvas in the annotations side panel:

- AoR annotations are grouped by where they are on the page (head, left margin,
  in text, right margin, tail, full page) under collapsible headings with a small
  page diagram, after a page header with pagination and reader. DLMM annotations
  are grouped into Illustration, Transcription (one entry per column) and
  Commentary.
- The annotation body `styleClass` (IIIF cookbook recipe 0045, see
  [`docs/iiif-generation.md`](../docs/iiif-generation.md#annotation-bodies)) becomes
  the CSS classes of each entry. They are styled by `public/styles/aor.css` and
  `public/styles/dlmm.css`, which `createSiteViewer()` links from
  `<base>/styles/<collectionId>.css`, in the same way collection logos come from
  `public/logo/`. rosa-tool's `--annotation-stylesheet-url` points the IIIF
  annotations' `stylesheet` at the same files, so other viewers can use them too.
  `src/plugins/annotations/annotations.css` has the layout shared by both.
- Editorial notes in transcriptions become an asterisk that shows the note on
  hover, keyboard focus or tap.

**Why `dompurify` is a direct dependency.** Mirador sanitizes annotation HTML
with its `iiif` rule set, which keeps only `a b br i img p span`. It drops
`small`, `sub` and `sup`, which the IIIF HTML recipe allows and the Rosa
annotations use for metadata, line numbers and notes, and Mirador's rule sets
cannot be extended (only `iiif`, `liberal`, `mirador2` and `noHtml` exist). The
plugin therefore sanitizes annotation bodies itself, with DOMPurify configured to
the recipe's tag and attribute list (`src/plugins/annotations/utils/sanitize.js`).
The version is pinned to the one Mirador depends on. Mirador's build inlines its
own copy, so the site bundle carries DOMPurify twice (about 25 kB). If a later
Mirador version allows configuring its sanitizer, this dependency can go.

### Book descriptions

The book description plugin (`src/plugins/description/`) adds a coffee-mug tab,
after the built-in tabs, to the sidebar of windows whose manifest has a TEI
`seeAlso` (profile `http://www.tei-c.org/ns/1.0` or format
`application/tei+xml`). rosa-tool writes it for DLMM books with an English
description, see
[`docs/iiif-generation.md`](../docs/iiif-generation.md#book-descriptions). Other
books, including all AoR books, get no tab.

- The panel fetches the TEI file and shows the `<note>`s of `<notesStmt>` as
  collapsible sections headed by their `rend` ("BASIC INFORMATION" becomes
  "Basic information"). Notes without `rend`, such as "Description by …", are
  shown as credit lines. The quire collation (`list` of `item n="…"`) is a
  two-column list.
- `<locus>` folio references that match a canvas label are links that show that
  canvas. A reference without r/v is taken as the recto (`9` → `9r`), and a range
  goes to its first folio (`from`). References that match nothing, such as
  `21 bis` or flyleaves, stay plain text.
- The TEI is converted to React elements, never to HTML, so nothing from the
  file is injected into the page.
- The panel uses the RosaAnnotations classes and custom properties
  (`.rosa-annos`, `--rosa-anno-*`), so `public/styles/dlmm.css` styles it too.
  `src/plugins/description/description.css` has the rest.
- Mirador shows plugin tabs whenever they are registered. To hide the tab, a
  wrap plugin on `WindowSideBarButtons` renders the buttons with a plugin map
  that leaves it out. The tab tooltip comes from the plugin's `config`
  (`translations.en.openCompanionWindow_bookDescription`).

The tests that parse XML use jsdom (`// @vitest-environment jsdom`), because
happy-dom, the default test environment, parses `application/xml` as HTML.

### URLs and browser history

The sites keep their state in the URL fragment, so a page can be bookmarked and
the browser's back and forward buttons work (`src/site/history/`). The URL
describes the search view when it is shown, and otherwise the book in the
focused window, however many windows are open. Search parameters are inside the
fragment too, because WordPress would treat a query string such as
`?s=relevance&m=30` as its own.

| View | URL |
|------|-----|
| Browse a collection | `#rose` |
| Browse by category | `#rose?f.date=15th+century&f.has_transcription=true` |
| Browse selected books (Name filter) | `#dlmm?f.id=rose.Douce195&f.id=pizan.Francais1243` |
| Simple search | `#rose/search?q=green` |
| Advanced search | `#rose/search?q=(description:'sword'+%26+title:'x')&type=advanced` |
| Advanced search, value plus extra text | `#aor/search?q=mark:'dash'%2B'regnum'&type=advanced` |
| Thumbnail gallery | `#rose/Arras845/thumb` |
| Scroll view | `#rose/Arras845/scroll` |
| Single image | `#rose/Add12042/001v/image` |
| Reading (two-page) view | `#rose/Arras845/015r/opening` |

Browse and search URLs also take `s` (sort: `relevance`, `page`, `title`,
`title_desc`), `o` (offset of the first result) and `m` (results per page).
Parameters with default values are left out. A page is named by its image
(`Add12042.001v` → `001v`), or `i<index>` for a canvas without an image of
its own.

Opening a book, a new view of it, a search, a category or another page of results
adds a history entry. Turning pages, switching between open windows and closing
a window update the URL in place. Going back to a book focuses the window that
shows it, or opens one if it was closed. Going back to a search shows the search
view and leaves the open windows alone.

The paths are the ones the previous AoR and DLMM viewers used, so their
bookmarks still work. Their search URLs, which put the parameters before the
fragment (`?q=green&m=30&s=relevance&type=basic&service=rose#rose/search`), are
rewritten into the new form. Their search-within-a-book URLs run the query
across the collection. Renamed advanced search fields (`repo`, `place`, `book`)
are mapped to their current names. WordPress handles the query string of those
old search bookmarks before the viewer loads, so they only reach the viewer if
WordPress serves the viewer page for them.

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

    // Optional: Collection selector labels by collection ID. A labelled
    // collection is shown with the logo {logoBaseUrl}/{id}.jpg.
    collectionLabels: { rose: 'Roman de la Rose Digital Library' },
    
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

    // Optional: View opened by clicking a book result: 'single', 'book' or
    // 'gallery' (default: 'book'). Thumbnails and page results open 'single'.
    bookClickView: 'book',
    
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
| `collectionLabels` | object | No | - | Collection selector labels by collection id. A collection with a label is shown with the logo `{logoBaseUrl}/{id}.jpg`; others are shown by id with no logo |
| `imageBaseUrl` | string | No | - | IIIF Image API base URL |
| `thumbnailTemplate` | string | No | (see above) | URL template for thumbnails |
| `thumbnailWidth` | number | No | `80` | Thumbnail width in pixels |
| `logoBaseUrl` | string | No | `'/logo'` | Base URL for collection and book logos |
| `enableFacets` | boolean | No | `true` | Enable faceted browsing |
| `bookClickView` | string | No | `'book'` | View opened by clicking a book result row: `'single'` or `'book'` at the start of the book, or `'gallery'`. Clicking a thumbnail in a book row, or a page result, always opens that page in single image view |
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
│   ├── logo/                 # Collection logos, copied next to each built site
│   └── styles/               # Annotation stylesheets aor.css, dlmm.css (also referenced from IIIF)
├── src/
│   ├── index.js              # Library entry point (createViewer)
│   ├── config.js             # Default configuration
│   ├── site/                 # createSiteViewer, site config resolution, shared site CSS
│   │   └── history/          # URL fragment <-> viewer state, browser history
│   ├── i18n/                 # Translations
│   ├── styles/
│   ├── test/                 # Vitest setup and shared mocks
│   └── plugins/
│       ├── annotations/      # RosaAnnotations plugin: grouped, styled annotations panel
│       ├── description/      # Book description plugin: TEI notesStmt side panel
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
URL so that `<site-url>/index.html`, `<site-url>/styles/` and `<site-url>/iiif/` resolve. The
Opensearch endpoint must allow the site's origin in its CORS configuration.

The GitHub Actions workflows automate this: `Deploy Stage` publishes both sites
to GitHub Pages and `Release` attaches `aor-site.tar.gz` and `dlmm-site.tar.gz`
to a release. See [`docs/deployment.md`](../docs/deployment.md).

## Dependencies

- [Mirador](https://github.com/ProjectMirador/mirador) - IIIF viewer
- [DOMPurify](https://github.com/cure53/DOMPurify) - Annotation HTML sanitizing (see [Annotations](#annotations))
- [React](https://reactjs.org/) - UI framework
- [MUI](https://mui.com/) - Component library
- [Vite](https://vitejs.dev/) - Build tool
- [Vitest](https://vitest.dev/) - Testing framework

## Documentation

- [Search Documentation](../docs/search.md) - JHSearch service specification
- [Opensearch Indexes](../docs/opensearch-indexes.md) - Index field definitions

## License

See the LICENSE file in the repository root.

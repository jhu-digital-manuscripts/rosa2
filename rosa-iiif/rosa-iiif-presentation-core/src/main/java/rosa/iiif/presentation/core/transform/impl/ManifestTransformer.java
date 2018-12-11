package rosa.iiif.presentation.core.transform.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.ObjectRef;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Rights;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.Within;

public class ManifestTransformer implements TransformerConstants {
    private final SequenceTransformer sequenceTransformer;
    private final PresentationUris pres_uris;
    
    public ManifestTransformer(PresentationUris pres_uris,
                               SequenceTransformer sequenceTransformer,
                               RangeTransformer rangeTransformer) {
        this.pres_uris = pres_uris;
        this.sequenceTransformer = sequenceTransformer;
    }

    public Manifest transform(BookCollection collection, Book book) {
        return buildManifest(collection, book);
    }

    /**
     * Transform a Book in the archive to a IIIF manifest.
     *
     * @param collection book collection holding the book
     * @param book book to manifest
     * @return the manifest
     */
    private Manifest buildManifest(BookCollection collection, Book book) {
        Manifest manifest = new Manifest();

        manifest.setId(pres_uris.getManifestURI(collection.getId(), book.getId()));
        manifest.setType(SC_MANIFEST);
        manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        manifest.setDefaultSequence(sequenceTransformer.transform(collection, book, DEFAULT_SEQUENCE_LABEL));
        // setSequences(...) not used, as it sets references to other sequences

        String lc = "en";
        BiblioData bd = book.getBiblioData(lc);
        manifest.setLabel(bd.getCommonName(), lc);
        manifest.setDescription(bd.getRepository() + ", " + bd.getShelfmark(), lc);

        Rights preziRights = new Rights();
        if (book.getLicenseUrl() != null) {
            preziRights.setLicenseUris(new String[] {book.getLicenseUrl()});
        }
        if (book.getPermission(lc) != null && book.getPermission(lc).getPermission() != null) {
            // Tolerate lack of permission data
            preziRights.addAttribution(book.getPermission(lc).getPermission(), lc);
        }

        manifest.setRights(preziRights);
        manifest.setViewingHint(ViewingHint.PAGED);

        manifest.setMetadata(transformMetadata(book, new String[]{lc}));
        // TODO Set manifest thumbnail, set to thumbnail for default sequence
        /*
         * Set 'within' property to point this manifest to its parent collections.
         * TODO load these collections to inspect collection hierarchy?
         * {
         *      "@id" : "manifest",
                "@type: "sc:Manifest",
                ...
                "within": {
                    "@id": "parent-collection",
                    "@type": "sc:Collection",
                    "within": {
                        "@id": "top-collection",
                        "@type": "sc:Collection"
                    }
                }
         * }
         */
        Within parent = new Within(
                pres_uris.getCollectionURI(collection.getId()),
                SC_COLLECTION,
                collection.getLabel()
        );
        manifest.setWithin(parent);
        // TODO ranges

        // Add search service
        manifest.addService(new Service(
                JHSearchService.CONTEXT_URI,
                pres_uris.getManifestURI(collection.getId(), book.getId()) + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE,
                manifest.getLabel(lc)
        ));
        manifest.addService(new Service(
                JHSearchService.CONTEXT_URI,
                pres_uris.getCollectionURI(collection.getId()) + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE,
                collection.getLabel()
        ));

        if (book.getBookDescription("en") != null) {
            manifest.setRelatedUri(pres_uris.getStaticResourceUri(collection.getId(), book.getId(),
                    book.getBookDescription("en").getId()));
            manifest.setRelatedFormat("application/xml");
        }

        return manifest;
    }

    /**
     * Handle the book's structured metadata and manifest it into Manifest metadata.
     *
     * @param book book
     * @param languages languages available
     */
    protected Map<String, HtmlValue> transformMetadata(Book book, String[] languages) {
        Map<String, HtmlValue> map = new HashMap<>();

        for (String lang : languages) {
            BookMetadata md = book.getBookMetadata();
            BiblioData bd = book.getBiblioData(lang);

            putMetadata(KEY_CURRENT_LOC, bd.getCurrentLocation(), lang, map);
            putMetadata(KEY_REPO, bd.getRepository(), lang, map);
            putMetadata(KEY_SHELFMARK, bd.getShelfmark(), lang, map);
            putMetadata(KEY_ORIGIN, bd.getOrigin(), lang, map);

            putMetadata(KEY_WIDTH, md.getWidth(), lang, map);
            putMetadata(KEY_HEIGHT, md.getHeight(), lang, map);
            putMetadata(KEY_YEAR_START, md.getYearStart(), lang, map);
            putMetadata(KEY_YEAR_END, md.getYearEnd(), lang, map);
            putMetadata(KEY_NUM_PAGES, md.getNumberOfPages(), lang, map);
            putMetadata(KEY_NUM_ILLS, md.getNumberOfIllustrations(), lang, map);
            putMetadata(KEY_TITLE, bd.getTitle(), lang, map);
            putMetadata(KEY_DATE, bd.getDateLabel(), lang, map);

            if (md.getDimensionsString() != null
                    && !md.getDimensionsString().replaceAll("\\s+", "").equals("-1x-1")) {
                putMetadata(KEY_DIMS, md.getDimensionsString(), lang, map);
            }

            putMetadata(KEY_DIM_UNITS, md.getDimensionUnits(), lang, map);
            putMetadata(KEY_TYPE, bd.getType(), lang, map);
            putMetadata(KEY_COMMON_NAME, bd.getCommonName(), lang, map);
            putMetadata(KEY_MATERIAL, bd.getMaterial(), lang, map);
            putMetadata(KEY_READER, bd.getReaders(), lang, map);
            putMetadata(KEY_AUTHOR, bd.getAuthors(), lang, map);

            String sites = Arrays.stream(bd.getAorWebsite())
                    .map(url -> "<a target=\"_blank\" href=\"" + url + "\">" + url + "</a>")
                    .collect(Collectors.joining(", "));
            map.put("AORWebsite", new HtmlValue(sites, lang));
            // TODO book texts
        }

        return map;
    }

    private void putMetadata(String key, int value, String lang, Map<String, HtmlValue> map) {
        if (value != -1) {
            putMetadata(key, String.valueOf(value), lang, map);
        }
    }

    private void putMetadata(String key, String[] value, String lang, Map<String, HtmlValue> map) {
        if (value != null && value.length > 0) {
            putMetadata(key, String.join(", ", value), lang, map);
        }
    }

    private void putMetadata(String key, String value, String lang, Map<String, HtmlValue> map) {
        if (value != null && !value.isEmpty()) {
            map.put(key, new HtmlValue(value, lang));
        }
    }

    private void putMetadata(String key, ObjectRef[] value, String lang, Map<String, HtmlValue> map) {
        if (value != null && value.length > 0) {
            String cnt = Arrays.stream(value).map(this::stringify).collect(Collectors.joining(", "));
            map.put(key, new HtmlValue(cnt, lang));
        }
    }

//    private void putMetadata(String key, ObjectRef value, String lang, Map<String, HtmlValue> map) {
//        if (value == null || value.getName() == null || value.getName().isEmpty()) {
//            return;
//        }
//
//        map.put(key, new HtmlValue(stringify(value), lang));
//    }

    private String stringify(ObjectRef obj) {
        if (obj == null || obj.getName() == null || obj.getName().isEmpty()) {
            return null;
        }

        if (obj.getUri() == null || obj.getUri().isEmpty()) {
            return obj.getName();
        } else {
            return "<a target=\"_blank\" href=\"" + obj.getUri() + "\">" + obj.getName() + "</a>";
        }
    }
}

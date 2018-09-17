package rosa.iiif.presentation.core.transform.impl;

import java.util.HashMap;
import java.util.Map;

import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Rights;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.Within;

public class ManifestTransformer extends BasePresentationTransformer {
    private final SequenceTransformer sequenceTransformer;

    public ManifestTransformer(IIIFPresentationRequestFormatter presRequestFormatter,
                               SequenceTransformer sequenceTransformer,
                               RangeTransformer rangeTransformer) {
        super(presRequestFormatter);
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

            map.put("currentLocation", new HtmlValue(bd.getCurrentLocation(), lang));
            map.put("repository", new HtmlValue(bd.getRepository(), lang));
            map.put("shelfmark", new HtmlValue(bd.getShelfmark(), lang));
            map.put("origin", new HtmlValue(bd.getOrigin(), lang));

            if (md.getWidth() != -1) {
                map.put("width", new HtmlValue(md.getWidth() + "", lang));
            }
            if (md.getHeight() != -1) {
                map.put("height", new HtmlValue(md.getHeight() + "", lang));
            }
            if (md.getYearStart() != -1) {
                map.put("yearStart", new HtmlValue(md.getYearStart() + "", lang));
            }
            if (md.getYearEnd() != -1) {
                map.put("yearEnd", new HtmlValue(md.getYearEnd() + "", lang));
            }
            if (md.getNumberOfPages() != -1) {
                map.put("numberOfPages", new HtmlValue(md.getNumberOfPages() + "", lang));
            }
            if (md.getNumberOfIllustrations() != -1) {
                map.put("numberOfIllustrations", new HtmlValue(md.getNumberOfIllustrations() + "", lang));
            }
            if (bd.getTitle() != null) {
                map.put("title", new HtmlValue(bd.getTitle(), lang));
            }
            if (bd.getDateLabel() != null) {
                map.put("date", new HtmlValue(bd.getDateLabel(), lang));
            }
            if (md.getDimensionsString() != null && !md.getDimensionsString().replaceAll("\\s+", "").equals("-1x-1")) {
                map.put("dimensions", new HtmlValue(md.getDimensionsString(), lang));
            }
            if (md.getDimensionUnits() != null) {
                map.put("dimensionUnits", new HtmlValue(md.getDimensionUnits(), lang));
            }
            if (bd.getType() != null) {
                map.put("type", new HtmlValue(bd.getType(), lang));
            }
            if (bd.getCommonName() != null) {
                map.put("commonName", new HtmlValue(bd.getCommonName(), lang));
            }
            if (bd.getMaterial() != null) {
                map.put("material", new HtmlValue(bd.getMaterial(), lang));
            }

            if (bd.getReaders().length > 0) {
                map.put("reader", new HtmlValue(bd.getReaders()[0], lang));
            }
            if (bd.getAuthors().length > 0) {
                map.put("author", new HtmlValue(bd.getAuthors()[0], lang));
            }
            


            // TODO book texts
        }

        return map;
    }
}

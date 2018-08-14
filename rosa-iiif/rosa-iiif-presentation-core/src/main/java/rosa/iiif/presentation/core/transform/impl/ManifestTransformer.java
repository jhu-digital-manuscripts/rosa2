package rosa.iiif.presentation.core.transform.impl;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.meta.BiblioData;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Rights;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.ViewingDirection;
import rosa.iiif.presentation.model.ViewingHint;
import rosa.iiif.presentation.model.Within;

public class ManifestTransformer extends BasePresentationTransformer implements Transformer<Manifest> {
    private final SequenceTransformer sequenceTransformer;

    @Inject
    public ManifestTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                               SequenceTransformer sequenceTransformer,
                               RangeTransformer rangeTransformer) {
        super(presRequestFormatter);
        this.sequenceTransformer = sequenceTransformer;
    }

    @Override
    public Manifest transform(BookCollection collection, Book book, String name) {
        return buildManifest(collection, book);
    }

    @Override
    public Class<Manifest> getType() {
        return Manifest.class;
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
        BookMetadata md = book.getBookMetadata(lc);
        manifest.setLabel(md.getCommonName(), lc);
        manifest.setDescription(md.getRepository() + ", " + md.getShelfmark(), lc);

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
            BookMetadata metadata = book.getBookMetadata(lang);

            map.put("currentLocation", new HtmlValue(metadata.getCurrentLocation(), lang));
            map.put("repository", new HtmlValue(metadata.getRepository(), lang));
            map.put("shelfmark", new HtmlValue(metadata.getShelfmark(), lang));
            map.put("origin", new HtmlValue(metadata.getOrigin(), lang));

            if (metadata.getWidth() != -1) {
                map.put("width", new HtmlValue(metadata.getWidth() + "", lang));
            }
            if (metadata.getHeight() != -1) {
                map.put("height", new HtmlValue(metadata.getHeight() + "", lang));
            }
            if (metadata.getYearStart() != -1) {
                map.put("yearStart", new HtmlValue(metadata.getYearStart() + "", lang));
            }
            if (metadata.getYearEnd() != -1) {
                map.put("yearEnd", new HtmlValue(metadata.getYearEnd() + "", lang));
            }
            if (metadata.getNumberOfPages() != -1) {
                map.put("numberOfPages", new HtmlValue(metadata.getNumberOfPages() + "", lang));
            }
            if (metadata.getNumberOfIllustrations() != -1) {
                map.put("numberOfIllustrations", new HtmlValue(metadata.getNumberOfIllustrations() + "", lang));
            }
            if (metadata.getTitle() != null) {
                map.put("title", new HtmlValue(metadata.getTitle(), lang));
            }
            if (metadata.getDate() != null) {
                map.put("date", new HtmlValue(metadata.getDate(), lang));
            }
            if (metadata.getDimensions() != null && !metadata.getDimensions().replaceAll("\\s+", "").equals("-1x-1")) {
                map.put("dimensions", new HtmlValue(metadata.getDimensions(), lang));
            }
            if (metadata.getDimensionUnits() != null) {
                map.put("dimensionUnits", new HtmlValue(metadata.getDimensionUnits(), lang));
            }
            if (metadata.getType() != null) {
                map.put("type", new HtmlValue(metadata.getType(), lang));
            }
            if (metadata.getCommonName() != null) {
                map.put("commonName", new HtmlValue(metadata.getCommonName(), lang));
            }
            if (metadata.getMaterial() != null) {
                map.put("material", new HtmlValue(metadata.getMaterial(), lang));
            }

            if (book.getMultilangMetadata() != null && book.getMultilangMetadata().getBiblioDataMap().containsKey(lang)) {
                BiblioData bd = book.getMultilangMetadata().getBiblioDataMap().get(lang);

                if (bd.getReaders().length > 0) {
                    map.put("reader", new HtmlValue(bd.getReaders()[0], lang));
                }
                if (bd.getAuthors().length > 0) {
                    map.put("author", new HtmlValue(bd.getAuthors()[0], lang));
                }
            }


            // TODO book texts
        }

        return map;
    }
}

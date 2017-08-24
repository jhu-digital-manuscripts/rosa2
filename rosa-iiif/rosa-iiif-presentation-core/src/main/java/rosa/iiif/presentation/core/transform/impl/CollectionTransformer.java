package rosa.iiif.presentation.core.transform.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import rosa.archive.core.SimpleStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Image;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.Rights;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.Within;

public class CollectionTransformer extends BasePresentationTransformer {
    public static final String TOP_COLLECTION_LABEL = "All JHU IIIF Collections";
    public static final String TOP_COLLECTION_NAME = "top";
    public static final String LANGUAGE_DEFAULT = "en";
    private static final int MAX_THUMBNAILS = 3;

    private SimpleStore store;

    private rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter;
    private ImageIdMapper idMapper;

    @Inject
    public CollectionTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                                 SimpleStore store,
                                 rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                                 ImageIdMapper idMapper) {
        super(presRequestFormatter);
        this.store = store;
        this.imageRequestFormatter = imageRequestFormatter;
        this.idMapper = idMapper;
    }

    public Collection collection(BookCollection collection) {
        Collection col = new Collection();

        col.setId(urlId(collection.getId(), null, collection.getId(), PresentationRequestType.COLLECTION));
        col.setLabel(collection.getLabel(), LANGUAGE_DEFAULT);
        col.setType(SC_COLLECTION);

        if (collection.getDescription() != null) {
            col.setDescription(new HtmlValue(collection.getDescription()));
        }

        col.setManifests(getBookRefs(collection));

        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                col.getId() + JHSearchService.RESOURCE_PATH,    // ID is already transformed above
                IIIF_SEARCH_PROFILE,
                col.getLabel(LANGUAGE_DEFAULT)
        ));
        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                urlId(TOP_COLLECTION_NAME, null, TOP_COLLECTION_NAME, PresentationRequestType.COLLECTION)
                        + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE,
                TOP_COLLECTION_LABEL
        ));

        List<Reference> childList = new ArrayList<>();
        for (String child : collection.getChildCollections()) {
            try {
                BookCollection childCol = store.loadBookCollection(child);

                childList.add(new Reference(
                        childCol.getId(),
                        new TextValue(childCol.getLabel(), LANGUAGE_DEFAULT),
                        IIIFNames.SC_COLLECTION
                ));
            } catch (IOException e) {}
        }
        col.setCollections(childList);

        List<Reference> parentList = new ArrayList<>();
        for (String parent : collection.getParentCollections()) {
            try {
                BookCollection parentCol = store.loadBookCollection(parent);
                parentList.add(new Reference(
                        parentCol.getId(),
                        new TextValue(parentCol.getLabel(), LANGUAGE_DEFAULT),
                        IIIFNames.SC_COLLECTION
                ));
            } catch (IOException e) {}
        }
        col.setWithin(new Within(
                urlId(TOP_COLLECTION_NAME, null, TOP_COLLECTION_NAME, PresentationRequestType.COLLECTION)
        ));

        return col;
    }

    // TODO modify once 'root' collection has been added to archive
    public Collection topCollection(List<BookCollection> collections) {
        Collection col = new Collection();

        col.setId(urlId(TOP_COLLECTION_NAME, null, TOP_COLLECTION_NAME, PresentationRequestType.COLLECTION));
        col.setLabel(TOP_COLLECTION_LABEL, LANGUAGE_DEFAULT);
        col.setDescription("Top level collection bringing together all other collections in this archive.", "en");
        col.setType(SC_COLLECTION);

        List<Reference> cols = new ArrayList<>();
        for (BookCollection c : collections) {
            Reference ref = new Reference();

            ref.setType(SC_COLLECTION);
            ref.setLabel(new TextValue(c.getLabel(), LANGUAGE_DEFAULT));
            ref.setReference(urlId(c.getId(), null, c.getId(), PresentationRequestType.COLLECTION));

            cols.add(ref);
        }

        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                col.getId() + JHSearchService.RESOURCE_PATH,
                IIIF_SEARCH_PROFILE
        ));

        col.setCollections(cols);
        return col;
    }

    /**
     * Get a list of references to the manifests in a collection. These references
     * may be decorated with metadata from the manifests.
     *
     * @param collection the book collection
     * @return list of refs
     */
    private List<Reference> getBookRefs(BookCollection collection) {
        List<Reference> refs = new ArrayList<>();
        for (String title : collection.books()) {
            Reference ref = new Reference();

            ref.setType(SC_MANIFEST);
            ref.setReference(urlId(collection.getId(), title, null, PresentationRequestType.MANIFEST));

            try {
                Book b = store.loadBook(collection.getId(), title);

                BookMetadata bm = b.getBookMetadata("en");

                Map<String, HtmlValue> map = new HashMap<>();
                if (bm.getCommonName() != null && !bm.getCommonName().isEmpty()) {
                    ref.setLabel(new TextValue(bm.getCommonName(), "en"));
                } else if (bm.getTitle() != null && !bm.getTitle().isEmpty()) {
                    ref.setLabel(new TextValue(bm.getTitle(), "en"));
                } else {
                    ref.setLabel(new TextValue(title, "en"));
                }

                map.put("Current Location", new HtmlValue(bm.getCurrentLocation(), "en"));
                map.put("Repository", new HtmlValue(bm.getRepository(), "en"));
                map.put("Shelfmark", new HtmlValue(bm.getShelfmark(), "en"));
                map.put("Origin", new HtmlValue(bm.getOrigin(), "en"));
                if (bm.getTitle() != null && !bm.getTitle().isEmpty()) {
                    map.put("Title", new HtmlValue(bm.getTitle(), "en"));
                }
                if (bm.getDate() != null && !bm.getTitle().isEmpty()) {
                    map.put("Date", new HtmlValue(bm.getDate(), "en"));
                }

                ref.setMetadata(map);

                ref.setThumbnails(getThumbnails(collection, b));
            } catch (IOException e) {
                ref.setLabel(new TextValue(title, "en"));
            }

            refs.add(ref);
        }
        return refs;
    }

    private List<Image> getThumbnails(BookCollection collection, Book book) {
        List<Image> list = new ArrayList<>();
        int i = 0;

        for (BookImage image : book.getImages()) {
            if (image.getLocation() == BookImageLocation.BODY_MATTER && !image.isMissing()) {
                String id = imageRequestFormatter.format(idMapper.mapId(collection, book, image.getId()));

                list.add(new Image(
                        id,
                        new IIIFImageService(IIIF_IMAGE_CONTEXT, id, IIIF_IMAGE_PROFILE_LEVEL2,
                                image.getWidth(), image.getHeight(), -1, -1, null)
                ));

                if (i++ > MAX_THUMBNAILS) {
                    break;
                }
            }
        }

        return list;
    }
}

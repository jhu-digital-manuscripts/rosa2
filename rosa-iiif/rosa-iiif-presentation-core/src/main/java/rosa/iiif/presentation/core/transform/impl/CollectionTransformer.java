package rosa.iiif.presentation.core.transform.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import rosa.archive.core.SimpleStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Image;
import rosa.iiif.presentation.model.Reference;
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

        col.setId(pres_uris.getCollectionURI(collection.getId()));
        col.setLabel(collection.getLabel(), LANGUAGE_DEFAULT);
        col.setType(SC_COLLECTION);

        if (collection.getDescription() != null) {
            col.setDescription(new HtmlValue(collection.getDescription()));
        }

        col.setManifests(getBookRefs(collection));

        // Add search service for THIS collection
        col.addService(new Service(
                JHSearchService.CONTEXT_URI,
                col.getId() + JHSearchService.RESOURCE_PATH,    // ID is already transformed above
                IIIF_SEARCH_PROFILE,
                col.getLabel(LANGUAGE_DEFAULT)
        ));

        List<Reference> childList = new ArrayList<>();
        for (String child : collection.getChildCollections()) {
            try {
                BookCollection childCol = store.loadBookCollection(child);
                if (childCol == null) {
                    continue;
                }
                childList.add(new Reference(
                        pres_uris.getCollectionURI(childCol.getId()),       
                        new TextValue(childCol.getLabel(), LANGUAGE_DEFAULT),
                        IIIFNames.SC_COLLECTION
                ));
            } catch (IOException e) {}
        }
        col.setCollections(childList);

        List<Within> parents = new ArrayList<>();
        for (String parent : collection.getParentCollections()) {
            try {
                BookCollection parentCol = store.loadBookCollection(parent);
                if (parentCol == null) {
                    continue;
                }
                // Add references and search services for parent collections
                String parentURI = pres_uris.getCollectionURI(parentCol.getId());
                parents.add(new Within(parentURI, SC_COLLECTION, parentCol.getLabel()));
                col.addService(new Service( JHSearchService.CONTEXT_URI, parentURI, IIIF_SEARCH_PROFILE, parentCol.getLabel()));
            } catch (IOException e) {}
        }
        col.setWithin(parents.toArray(new Within[parents.size()]));

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
            ref.setReference(pres_uris.getManifestURI(collection.getId(), title));

            try {
                Book b = store.loadBook(collection.getId(), title);
                BookMetadata bm = b.getBookMetadata(LANGUAGE_DEFAULT);

                Map<String, HtmlValue> map = new HashMap<>();
                if (hasContent(bm.getCommonName())) {
                    ref.setLabel(new TextValue(bm.getCommonName(), LANGUAGE_DEFAULT));
                } else if (hasContent(bm.getTitle())) {
                    ref.setLabel(new TextValue(bm.getTitle(), LANGUAGE_DEFAULT));
                } else {
                    ref.setLabel(new TextValue(title, LANGUAGE_DEFAULT));
                }

                map.put("Current Location", new HtmlValue(bm.getCurrentLocation(), LANGUAGE_DEFAULT));
                map.put("Repository", new HtmlValue(bm.getRepository(), LANGUAGE_DEFAULT));
                map.put("Shelfmark", new HtmlValue(bm.getShelfmark(), LANGUAGE_DEFAULT));
                map.put("Origin", new HtmlValue(bm.getOrigin(), LANGUAGE_DEFAULT));
                if (hasContent(bm.getTitle())) {
                    map.put("Title", new HtmlValue(bm.getTitle(), LANGUAGE_DEFAULT));
                }
                if (hasContent(bm.getDate())) {
                    map.put("Date", new HtmlValue(bm.getDate(), LANGUAGE_DEFAULT));
                }
                map.put("pageCount", new HtmlValue(String.valueOf(b.getImages().getImages().size()), LANGUAGE_DEFAULT));

                ref.setMetadata(map);

                if (b.getMultilangMetadata() != null && b.getMultilangMetadata().getBiblioDataMap() != null
                        && b.getMultilangMetadata().getBiblioDataMap().containsKey(LANGUAGE_DEFAULT)) {
                    String[] auths = b.getMultilangMetadata().getBiblioDataMap().get(LANGUAGE_DEFAULT).getAuthors();
                    if (auths != null && auths.length > 0) {
                        ref.addSortingTag("0" + auths[0]);
                    }
                }
                if (hasContent(bm.getCommonName())) {
                    ref.addSortingTag("1" + bm.getCommonName());
                }
                ref.addSortingTag("2" + ref.getReference());

                ref.setThumbnails(getThumbnails(collection, b));
            } catch (IOException e) {
                ref.setLabel(new TextValue(title, LANGUAGE_DEFAULT));
            }

            refs.add(ref);
        }
//        refs.sort((o1, o2) -> {
//            String t1 = o1.getSortingTag();
//            String t2 = o2.getSortingTag();
//
//            if (t1 == null && t2 == null) {
//                return 0;
//            } else if (t1 == null) {
//                return -1;
//            } else if (t2 == null) {
//                return 1;
//            } else {
//                return t1.compareTo(t2);
//            }
//        });
        refs.sort(Comparator.comparing(Reference::getSortingTag));
        return refs;
    }

    private List<Image> getThumbnails(BookCollection collection, Book book) {
        List<Image> list = new ArrayList<>();
        int i = 0;

        ImageList images = book.getCroppedImages();
        boolean cropped = true;
        
        if (images == null) {
            images = book.getImages();
            cropped = false;
        }
        
        for (BookImage image : book.getImages()) {
            if (image.getLocation() == BookImageLocation.BODY_MATTER && !image.isMissing()) {
                String id = imageRequestFormatter.format(idMapper.mapId(collection, book, image.getId(), cropped));

                Image thumb = new Image(
                        id,
                        new IIIFImageService(IIIF_IMAGE_CONTEXT, id, IIIF_IMAGE_PROFILE_LEVEL2,
                                image.getWidth(), image.getHeight(), -1, -1, null)
                );
                thumb.setDepicts(pres_uris.getCanvasURI(collection.getId(), book.getId(), image.getName()));

                list.add(thumb);

                if (i++ > MAX_THUMBNAILS) {
                    break;
                }
            }
        }

        return list;
    }

    private boolean hasContent(String str) {
        return str != null && !str.isEmpty();
    }
}

package rosa.iiif.presentation.core.transform.impl;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.core.IIIFPresentationCache;
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
//    public static final String TOP_COLLECTION_LABEL = "All JHU IIIF Collections";
//    public static final String TOP_COLLECTION_NAME = "top";
    private static final String LANGUAGE_DEFAULT = "en";
    private static final int MAX_THUMBNAILS = 3;

    private rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter;
    private ImageIdMapper idMapper;
    private IIIFPresentationCache cache;

    public CollectionTransformer(IIIFPresentationCache cache, IIIFPresentationRequestFormatter presRequestFormatter,
                                 rosa.iiif.image.core.IIIFRequestFormatter imageRequestFormatter,
                                 ImageIdMapper idMapper) {
        super(presRequestFormatter);
        this.imageRequestFormatter = imageRequestFormatter;
        this.idMapper = idMapper;
        this.cache = cache;
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
            BookCollection childCol = cache.getBookCollection(child);
            
            if (childCol == null) {
                continue;
            }
            
            childList.add(new Reference(
                    pres_uris.getCollectionURI(childCol.getId()),       
                    new TextValue(childCol.getLabel(), LANGUAGE_DEFAULT),
                    IIIFNames.SC_COLLECTION
            ));
        }
        col.setCollections(childList);

        List<Within> parents = new ArrayList<>();
        for (String parent : collection.getParentCollections()) {
            BookCollection parentCol = cache.getBookCollection(parent);
            
            if (parentCol == null) {
                continue;
            }
            // Add references and search services for parent collections
            String parentURI = pres_uris.getCollectionURI(parentCol.getId());
            parents.add(new Within(parentURI, SC_COLLECTION, parentCol.getLabel()));
            col.addService(new Service( JHSearchService.CONTEXT_URI, parentURI, IIIF_SEARCH_PROFILE, parentCol.getLabel()));
        }
        col.setWithin(parents.toArray(new Within[0]));

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
        for (String book_id : collection.books()) {
            Reference ref = new Reference();

            ref.setType(SC_MANIFEST);
            ref.setReference(pres_uris.getManifestURI(collection.getId(), book_id));

            Book b = cache.getBook(collection, book_id);
            
            BiblioData bd = b.getBiblioData("en");
            
            Map<String, HtmlValue> map = new HashMap<>();

            if (bd.getCommonName() != null && !bd.getCommonName().isEmpty()) {
                ref.setLabel(new TextValue(bd.getCommonName(), "en"));
            } else if (bd.getTitle() != null && !bd.getTitle().isEmpty()) {
                ref.setLabel(new TextValue(bd.getTitle(), "en"));
            } else {
                ref.setLabel(new TextValue(book_id, LANGUAGE_DEFAULT));
            }

            map.put("pageCount", new HtmlValue(String.valueOf(b.getImages().getImages().size()), LANGUAGE_DEFAULT));

            String[] auths = bd.getAuthors();
            
            if (auths != null && auths.length > 0) {
                ref.addSortingTag("0" + auths[0]);
            }
            
            String[] readers = bd.getReaders();
            
            if (readers != null && readers.length > 0) {
                StringBuilder sb = new StringBuilder();
                
                for (int i = 0; i < readers.length; i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }
                    sb.append(readers[i]);
                }
                
                map.put("Reader", new HtmlValue(sb.toString(), LANGUAGE_DEFAULT));
            }
         

            map.put("Current Location", new HtmlValue(bd.getCurrentLocation(), "en"));
            map.put("Repository", new HtmlValue(bd.getRepository(), "en"));
            map.put("Shelfmark", new HtmlValue(bd.getShelfmark(), "en"));
            map.put("Origin", new HtmlValue(bd.getOrigin(), "en"));
            if (bd.getTitle() != null && !bd.getTitle().isEmpty()) {
                map.put("Title", new HtmlValue(bd.getTitle(), "en"));
            }
            if (bd.getDateLabel() != null && !bd.getDateLabel().isEmpty()) {
                map.put("Date", new HtmlValue(bd.getDateLabel(), "en"));
            }

            ref.setMetadata(map);

            if (hasContent(bd.getCommonName())) {
                ref.addSortingTag("1" + bd.getCommonName());
            }
            ref.addSortingTag("2" + ref.getReference());

            ref.setThumbnails(getThumbnails(collection, b));

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

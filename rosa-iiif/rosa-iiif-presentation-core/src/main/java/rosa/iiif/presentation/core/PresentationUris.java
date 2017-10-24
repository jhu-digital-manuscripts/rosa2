package rosa.iiif.presentation.core;

import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;


/**
 * Handle mapping of archive objects into recommended URI patterns for IIIF
 * Presentation API.
 *
 * For non-collection objects, the id is 'COLLECTION_ID.BOOK_ID'.
 */
public class PresentationUris {
    private final IIIFPresentationRequestFormatter formatter;

    public PresentationUris(IIIFPresentationRequestFormatter formatter) {
        this.formatter = formatter;
    }

    public String getCollectionURI(String collection) {
        return formatter.format(new PresentationRequest(null, collection, PresentationRequestType.COLLECTION));
    }

    public String getManifestURI(String collection, String book) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), null, PresentationRequestType.MANIFEST));
    }

    public String getAnnotationURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.ANNOTATION));
    }
    
    public String getAnnotationListURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.ANNOTATION_LIST));
    }
    
    public String getSequenceURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.SEQUENCE));
    }
    
    public String getLayerURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.LAYER));
    }
    
    public String getRangeURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.RANGE));
    }
    
    public String getCanvasURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(get_presentation_id(collection, book), name,
                PresentationRequestType.CANVAS));
    }

    private String get_presentation_id(String collection, String book) {
        return collection + "." + book;
    }

    /**
     * @param id
     *            - presentation request id
     * @return collection id
     */
    public static String getCollectionId(String id) {
        int i = id.indexOf('.');

        if (i == -1) {
            return null;
        }

        return id.substring(0, i);
    }

    /**
     * @param id
     *            - presentation request id
     * @return book id
     */
    public static String getBookId(String id) {
        int i = id.indexOf('.');

        if (i == -1) {
            return null;
        }

        return id.substring(i + 1);
    }
}

package rosa.iiif.presentation.core;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * Handle mapping of archive objects into URIs for IIIF Presentation API.\
 * 
 * The general structure is COLLECTION / NAME? / NAME? / TYPE
 */
public class PresentationUris {
    private final IIIFPresentationRequestFormatter formatter;

    @Inject
    public PresentationUris(@Named("formatter.presentation") IIIFPresentationRequestFormatter formatter) {
        this.formatter = formatter;
    }

    public String getCollectionURI(String collection) {
        return formatter.format(new PresentationRequest(PresentationRequestType.COLLECTION, collection));
    }

    public String getManifestURI(String collection, String book) {
        return formatter.format(new PresentationRequest(PresentationRequestType.MANIFEST, collection, book));
    }

    public String getAnnotationURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.ANNOTATION, collection, book, name));
    }
    
    public String getAnnotationListURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.ANNOTATION_LIST, collection, book, name));
    }
    
    public String getSequenceURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.SEQUENCE, collection, book, name));
    }
    
    public String getRangeURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.RANGE, collection, book, name));
    }
    
    public String getLayerURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.LAYER, collection, book, name));
    }
    
    public String getCanvasURI(String collection, String book, String name) {
        return formatter.format(new PresentationRequest(PresentationRequestType.CANVAS, collection, book, name));
    }
}

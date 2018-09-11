package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;

import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.Range;

/**
 * An implementation of the IIIF Presentation API that transforms objects from
 * the archive model to the presentation model.
 * 
 * The IIIF presentation identifiers are "collection.book" where collection is
 * the name of a collection and book is an identifier of a book in the named
 * collection.
 * 
 * Objects are loaded from the archive and transformed into IIIF Presentation objects and then serialized.
 * To improve performance, BookCollections, Collections, Books, and Manifests are cached.
 */
public class ArchiveIIIFPresentationService implements IIIFPresentationService {    
    private final PresentationSerializer serializer;
    private final PresentationTransformer transformer;
    private final IIIFPresentationCache cache;

    /**
    *
    * @param cache loads and caches objects
    * @param jsonld_serializer serializer to write the response as json-ld
    * @param transformer transformer to transform archive data to IIIF presentation data
    * @param max_cache_size max number of objects to cache at a time
    */
   public ArchiveIIIFPresentationService(IIIFPresentationCache cache, PresentationSerializer jsonld_serializer, PresentationTransformer transformer) {
       this.serializer = jsonld_serializer;
       this.transformer = transformer;
       this.cache = cache;
   }

    @Override
    public boolean handle_request(String uri, OutputStream os) throws IOException {
        return false;
    }

    @Override
    public boolean handle_request(PresentationRequest req, OutputStream os) throws IOException {
        switch (req.getType()) {
            case ANNOTATION_LIST:
                return handle_annotation_list(req.getIdentifier(), os);
            case CANVAS:
                return handle_canvas(req.getIdentifier(), os);
            case COLLECTION:
                return handle_collection(req.getIdentifier(), os);
            case MANIFEST:
                return handle_manifest(req.getIdentifier(), os);
            case RANGE:
                return handle_range(req.getIdentifier(), os);
            default:
                throw new IOException("Unknown type: " + req.getType());
        }
    }
    
    private boolean handle_collection(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        Collection col = cache.get(col_id, Collection.class, () -> transformer.collection(cache.getBookCollection(col_id)));
        
        if (col == null) {
            return false;
        }

        serializer.write(col, os);
        
        return true;
    }

    private boolean handle_range(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        String book_id = identifier[1];
        String name = identifier[2];
        
        Range range = transformer.range(cache.getBookCollection(col_id), cache.getBook(col_id, book_id), name);
        
        if (range == null) {
            return false;
        }
        
        serializer.write(range, os);
        
        return true;
    }

    private boolean handle_manifest(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        String book_id = identifier[1];
        String id = col_id + book_id;
        
        Manifest man = cache.get(id, Manifest.class, () -> 
            transformer.manifest(cache.getBookCollection(col_id), cache.getBook(col_id, book_id)));
        
        if (man == null) {
            return false;
        }

        serializer.write(man, os);

        return true;
    }
    
    private boolean handle_canvas(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        String book_id = identifier[1];
        String name = identifier[2];
        
        Canvas canvas = transformer.canvas(cache.getBookCollection(col_id), cache.getBook(col_id, book_id), name);
        
        if (canvas == null) {
            return false;
        }
        
        serializer.write(canvas, os);
        
        return true;
    }

    private boolean handle_annotation_list(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        String book_id = identifier[1];
        String name = identifier[2];
        
        AnnotationList list = transformer.annotationList(cache.getBookCollection(col_id), cache.getBook(col_id, book_id), name);
        
        if (list == null) {
            return false;
        }
        
        serializer.write(list, os);
        
        return true;
    }
}

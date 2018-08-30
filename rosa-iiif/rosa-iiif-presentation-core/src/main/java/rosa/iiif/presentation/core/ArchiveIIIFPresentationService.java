package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import rosa.archive.core.SimpleStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
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
 * To improve performance some objects are cached in memory.
 */
public class ArchiveIIIFPresentationService implements IIIFPresentationService {
    private final static Logger logger = Logger.getLogger(ArchiveIIIFPresentationService.class.getName());
    
    private final SimpleStore store;
    private final PresentationSerializer serializer;
    private final PresentationTransformer transformer;
    private final ConcurrentHashMap<String, Object> cache;
    private final int max_cache_size;
    
    /**
     *
     * @param store the store to manipulate the archive
     * @param jsonld_serializer serializer to write the response as json-ld
     * @param transformer transformer to transform archive data to IIIF presentation data
     * @param max_cache_size max number of objects to cache at a time
     */
    public ArchiveIIIFPresentationService(SimpleStore store, PresentationSerializer jsonld_serializer, PresentationTransformer transformer, int max_cache_size) {
        this.store = store;
        this.serializer = jsonld_serializer;
        this.transformer = transformer;
        this.max_cache_size = max_cache_size;
        this.cache = new ConcurrentHashMap<>();
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

    // Return value in cache if present or updates cache with supplied value
    private <T> T get_cached(String id, Class<T> type, Supplier<T> supplier) {
        String key = id + "," + type.getName(); 
        T value = type.cast(cache.get(key));
        
        if (value == null) {
            value = supplier.get();
            
            if (cache.size() > max_cache_size) {
                cache.clear();
            }
            
            if (value != null) {
                cache.putIfAbsent(key, value);
            }
        }
        
        return value;
    }

    private BookCollection get_book_collection(String col_id) {
        return get_cached(col_id, BookCollection.class, () -> {
            try {
                return store.loadBookCollection(col_id);
            } catch (IOException e) {
                logger.log(Level.SEVERE,  "Loading collection " + col_id, e);
                return null;
            }
        });
    }
    
    private Book get_book(String col_id, String book_id) {
        // Do not bother caching books
        
        try {
            return store.loadBook(col_id, book_id);
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Loading book " + col_id + " " + book_id, e);
            return null;
        }
    }
    
    private boolean handle_collection(String[] identifier, OutputStream os) throws IOException {
        String col_id = identifier[0];
        Collection col = get_cached(col_id, Collection.class, () -> transformer.collection(get_book_collection(col_id)));
        
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
        String id = col_id + book_id + name;
        
        Range range = get_cached(id, Range.class, () -> 
            transformer.range(get_book_collection(col_id), get_book(col_id, book_id), name));
        
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
        
        Manifest man = get_cached(id, Manifest.class, () -> 
            transformer.manifest(get_book_collection(col_id), get_book(col_id, book_id)));
        
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
        String id = col_id + book_id + name;
        
        Canvas canvas = get_cached(id, Canvas.class, () -> 
            transformer.canvas(get_book_collection(col_id), get_book(col_id, book_id), name));
        
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
        String id = col_id + book_id + name;
        
        AnnotationList list = get_cached(id, AnnotationList.class, () -> 
            transformer.annotationList(get_book_collection(col_id), get_book(col_id, book_id), name));
        
        if (list == null) {
            return false;
        }
        
        serializer.write(list, os);
        
        return true;
    }
}

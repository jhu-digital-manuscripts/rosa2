package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import rosa.archive.core.Store;
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
import rosa.iiif.presentation.model.Sequence;

/**
 * An implementation of the IIIF Presentation API that transforms objects from
 * the archive model to the presentation model.
 * 
 * The IIIF presentation identifiers are "collection.book" where collection is
 * the name of a collection and book is an identifier of a book in the named
 * collection.
 */
public class ArchiveIIIFService implements IIIFService {
    private final Store store;
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
    public ArchiveIIIFService(Store store, PresentationSerializer jsonld_serializer, PresentationTransformer transformer, int max_cache_size) {
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
        case ANNOTATION:
            return handle_annotation(req.getId(), req.getName(), os);
        case ANNOTATION_LIST:
            return handle_annotation_list(req.getId(), req.getName(), os);
        case CANVAS:
            return handle_canvas(req.getId(), req.getName(), os);
        case COLLECTION:
            return handle_collection(req.getName(), os);
        case CONTENT:
            return handle_content(req.getId(), req.getName(), os);
        case LAYER:
            return handle_layer(req.getId(), req.getName(), os);
        case MANIFEST:
            return handle_manifest(req.getId(), os);
        case RANGE:
            return handle_range(req.getId(), req.getName(), os);
        case SEQUENCE:
            return handle_sequence(req.getId(), req.getName(), os);
        default:
            throw new IOException("Unknown type: " + req.getType());
        }
    }

    private boolean handle_sequence(String id, String name, OutputStream os) throws IOException {
        BookCollection collection = get_collection_from_id(id);
        Book book = get_book_from_id(id);

        if (collection == null || book == null) {
            return false;
        }

        Sequence seq = transformer.sequence(collection, book, name);

        serializer.write(seq, os);

        return false;
    }

    private boolean handle_range(String id, String name, OutputStream os) throws IOException {
        BookCollection col = get_collection_from_id(id);

        if (col == null) {
            return false;
        }

        Book book = get_book_from_id(id);

        if (book == null) {
            return false;
        }
        
        Range range = transformer.buildRange(col, book, name);
        
        if (range == null) {
            return false;
        }
        
        serializer.write(range, os);
        
        return true;
    }

    private boolean handle_manifest(String id, OutputStream os) throws IOException {
        BookCollection col = get_collection_from_id(id);

        if (col == null) {
            return false;
        }

        Book book = get_book_from_id(id);

        if (book == null) {
            return false;
        }

        Manifest man = lookupCache(book.getId(), Manifest.class);

        if (man == null) {
            man = transformer.manifest(col, book);
            updateCache(book.getId(), man);
        }

        serializer.write(man, os);

        return true;
    }

    private boolean handle_layer(String id, String name, OutputStream os) {
        return false;
    }

    private boolean handle_content(String id, String name, OutputStream os) {
        return false;
    }

    private String[] split_id(String id) {
        String[] parts = id.split("\\.");

        if (parts.length != 2) {
            return null;
        }

        return parts;
    }

    private String get_collection_id(String id) {
        String[] parts = split_id(id);

        if (parts == null) {
            return null;
        }

        return parts[0];
    }

    private String get_book_id(String id) {
        String[] parts = split_id(id);

        if (parts == null) {
            return null;
        }

        return parts[1];
    }

    // Id of object in cache must be unique for class
    
    private String cache_key(String id, Class<?> type) {
        return id + "," + type.getName();
    }
    
    private <T> T lookupCache(String id, Class<T> type) {
        return type.cast(cache.get(cache_key(id, type)));
    }
    
    private void updateCache(String id, Object value) {
        if (cache.size() > max_cache_size) {
            cache.clear();
        }
        
        cache.putIfAbsent(cache_key(id, value.getClass()), value);
    }
    
    private BookCollection load_book_collection(String col_id) throws IOException {
        BookCollection result = lookupCache(col_id, BookCollection.class);
        
        if (result == null) {
            result = store.loadBookCollection(col_id, null);
            updateCache(col_id, result);
        }
        
        return result;
    }
    
    private Book load_book(String col_id, String book_id) throws IOException {
        Book result = lookupCache(book_id, Book.class);
        
        if (result == null) {
            BookCollection col = load_book_collection(col_id);
            
            if (col == null) {
                return null;
            }
            
            result = store.loadBook(col, book_id, null);
            updateCache(book_id, result);
        }
        
        return result;
    }
    
    private BookCollection get_collection_from_id(String id) throws IOException {
        return load_book_collection(get_collection_id(id));
    }

    private Book get_book_from_id(String id) throws IOException {
        return load_book(get_collection_id(id), get_book_id(id));
    }

    private boolean handle_collection(String name, OutputStream os) throws IOException {
        if (name.equals("top")) {
            return handle_top_collection(os);
        }

        BookCollection col = load_book_collection(name);

        if (col == null) {
            return false;
        }

        Collection result = lookupCache(name, Collection.class); 

        if (result == null) {
            result = transformer.transform(col);
            updateCache(name, result);
        }
        
        serializer.write(result, os);
        
        return true;
    }

    private boolean handle_top_collection(OutputStream os) throws IOException {
        List<BookCollection> collections = new ArrayList<>();
        for (String name : store.listBookCollections()) {
            // Hack for current archive in rosetest under /mnt
            if (name.equals("cdrom")) {
                continue;
            }
            BookCollection col = store.loadBookCollection(name, null);
            if (col != null) {
                collections.add(col);
            }
        }

        if (collections.isEmpty()) {
            return false;
        }

        serializer.write(transformer.transform(collections), os);

        return true;
    }

    private boolean handle_canvas(String id, String name, OutputStream os) throws IOException {
        Book book = get_book_from_id(id);
        BookCollection collection = get_collection_from_id(id);

        if (book == null || collection == null) {
            return false;
        }

        Canvas canvas = transformer.canvas(collection, book, name);
        
        if (canvas == null) {
            return false;
        }

        serializer.write(canvas, os);

        return true;
    }

    private boolean handle_annotation_list(String id, String name, OutputStream os) throws IOException {
        BookCollection collection = get_collection_from_id(id);
        Book book = get_book_from_id(id);

        if (collection == null || book == null) {
            return false;
        }

        AnnotationList list = transformer.annotationList(collection, book, get_annotation_list_page(name),
                get_annotation_list_id(name));

        if (list == null) {
            return false;
        }

        serializer.write(list, os);

        return true;
    }

    private String get_annotation_list_page(String name) {
        String[] parts = split_id(name);

        if (parts == null) {
            return null;
        }

        return parts[0];
    }

    private String get_annotation_list_id(String name) {
        String[] parts = split_id(name);

        if (parts == null) {
            return null;
        }

        return parts[1];
    }

    private boolean handle_annotation(String id, String name, OutputStream os) {
        return false;
    }
}

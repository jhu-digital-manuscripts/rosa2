package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;

import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationRequest;

import com.google.inject.Inject;

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

    // TODO Caches for intermediate objects and/or whole serialization

    @Inject
    public ArchiveIIIFService(Store store, PresentationSerializer jsonld_serializer, PresentationTransformer transformer) {
        this.store = store;
        this.serializer = jsonld_serializer;
        this.transformer = transformer;
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

    private boolean handle_sequence(String id, String name, OutputStream os) {
        return false;
    }

    private boolean handle_range(String id, String name, OutputStream os) {
        return false;
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

        Manifest man = transformer.transform(col, book);

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

    private BookCollection get_collection_from_id(String id) throws IOException {
        return store.loadBookCollection(get_collection_id(id), null);
    }

    private Book get_book_from_id(String id) throws IOException {
        return store.loadBook(store.loadBookCollection(get_collection_id(id), null), get_book_id(id), null);
    }

    private boolean handle_collection(String name, OutputStream os) throws IOException {
        BookCollection col = store.loadBookCollection(name, null);

        if (col == null) {
            return false;
        }

        return true;
    }

    private boolean handle_canvas(String id, String name, OutputStream os) throws IOException {
        Book book = get_book_from_id(id);

        if (book == null) {
            return false;
        }

        ImageList images = book.getImages();
        BookImage canvas_image = null;

        for (BookImage image : images.getImages()) {
            if (image.getId().equals(name)) {
                canvas_image = image;
            }
        }

        if (canvas_image == null) {
            return false;
        }

        return true;
    }

    private boolean handle_annotation_list(String id, String name, OutputStream os) {
        return false;
    }

    private boolean handle_annotation(String id, String name, OutputStream os) {
        return false;
    }
}

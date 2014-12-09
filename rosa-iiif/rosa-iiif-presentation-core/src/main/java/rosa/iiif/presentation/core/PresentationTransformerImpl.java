package rosa.iiif.presentation.core;

import rosa.archive.core.store.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationBase;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class PresentationTransformerImpl implements PresentationTransformer {
    private Map<Class, Transformer> transformerMap;
    private Store archiveStore;

    // TODO use Guice to inject dependencies
    public PresentationTransformerImpl(Map<Class, Transformer> transformerMap, Store archiveStore) {
        this.transformerMap = transformerMap;
        this.archiveStore = archiveStore;
    }

    @Override
    public Book loadBook(String collection, String book) {
        try {
            return archiveStore.loadBook(collection, book, new ArrayList<String>());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public BookCollection loadCollection(String collection) {
        try {
            return archiveStore.loadBookCollection(collection, new ArrayList<String>());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Manifest manifest(String collection, String book) {
        return transform(collection, book, "manifest", Manifest.class);
    }

    @Override
    public Sequence sequence(String collection, String book, String sequence) {
        return transform(collection, book, sequence, Sequence.class);
    }

    @Override
    public Canvas canvas(String collection, String book, String canvas) {
        return transform(collection, book, canvas, Canvas.class);
    }

    @Override
    public Annotation imageResource(String collection, String book, String image) {
        return transform(collection, book, image, Annotation.class);
    }

    @Override
    public Annotation annotation(String collection, String book, String annotation) {
        return transform(collection, book, annotation, Annotation.class);
    }
// --------------------------------------------------------------------------------------------------------------
// ---------- ignore --------------------------------------------------------------------------------------------
// --------------------------------------------------------------------------------------------------------------
//    @Override
//    public String toJson(Manifest manifest) {
//        return transformToJson(manifest, Manifest.class);
//    }
//
//    @Override
//    public String toJson(Sequence sequence) {
//        return transformToJson(sequence, Sequence.class);
//    }
//
//    @Override
//    public String toJson(Canvas canvas) {
//        return transformToJson(canvas, Canvas.class);
//    }
//
//    @Override
//    public String toJson(Annotation annotation) {
//        return transformToJson(annotation, Annotation.class);
//    }
//
//    public <T> String transformToJson(T obj, Class<T> type) {
//        Transformer<T> transformer = getTransformer(type);
//        return transformer.toJson(obj);
//    }
// --------------------------------------------------------------------------------------------------------------
// --------------------------------------------------------------------------------------------------------------

    private <T extends PresentationBase> T transform(String collection, String book, String id, Class<T> type) {
        Transformer<T> transformer = getTransformer(type);

        return transformer.transform(
                loadCollection(collection),
                loadBook(collection, book),
                id
        );
    }

    @SuppressWarnings("unchecked")
    private <T> Transformer<T> getTransformer(Class<T> type) {
        return transformerMap.get(type);
    }
}
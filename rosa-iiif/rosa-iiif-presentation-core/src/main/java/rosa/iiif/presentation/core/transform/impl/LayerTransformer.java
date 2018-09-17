package rosa.iiif.presentation.core.transform.impl;

import java.util.ArrayList;
import java.util.List;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.AnnotationListType;
import rosa.iiif.presentation.model.Layer;

// TODO finish implementing
public class LayerTransformer implements TransformerConstants {
    private final PresentationUris pres_uris;
    
    public LayerTransformer(PresentationUris pres_uris) {
        this.pres_uris = pres_uris;
    }

    public Layer transform(BookCollection collection, Book book, String name) {
        return layer(collection, book, name);
    }

    /**
     * Build a layer specified by its name from data in a book and the collection
     * that contains it.
     *
     * @param collection book collection
     * @param book book
     * @param name name of the layer
     * @return a IIIF layer
     */
    private Layer layer(BookCollection collection, Book book, String name) {
        Layer layer = new Layer();

        layer.setId(pres_uris.getLayerURI(collection.getId(), book.getId(), name));
        layer.setType(SC_LAYER);
        layer.setLabel(name, "en");

        if (AnnotationListType.getType(name) != null) {
            List<String> otherContent = new ArrayList<>();
            for (BookImage image : book.getImages()) {
                String id = image.getId();

                otherContent.add(pres_uris.getAnnotationListURI(collection.getId(), book.getId(), annotationListName(id, name)));
            }
            layer.setOtherContent(otherContent);
        }

        return layer;
    }

    private String annotationListName(String page, String listType) {
        return page + (listType == null ? "" : "." + listType);
    }
}

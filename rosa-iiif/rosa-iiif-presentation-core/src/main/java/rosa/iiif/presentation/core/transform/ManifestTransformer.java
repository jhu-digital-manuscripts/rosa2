package rosa.iiif.presentation.core.transform;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.model.Manifest;

public class ManifestTransformer implements Transformer<Manifest> {
    @Override
    public Manifest transform(BookCollection collection, Book book, String id) {
        return null;
    }
}

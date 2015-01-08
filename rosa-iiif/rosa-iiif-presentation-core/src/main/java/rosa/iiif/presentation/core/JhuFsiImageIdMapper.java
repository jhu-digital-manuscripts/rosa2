package rosa.iiif.presentation.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.util.Map;

/**
 * An implementation of ImageIdMapper that knows how to find an ID of
 * an image on the JHU FSI image server, given its location in the JHU
 * archive.
 */
public class JhuFsiImageIdMapper implements ImageIdMapper {
    private final Map<String, String> idMap;

    public JhuFsiImageIdMapper(Map<String, String> idMap) {
        this.idMap = idMap;
    }

    @Override
    public String mapId(BookCollection collection, Book book, String imageId) {
        return idMap.get(collection.getId()) + "/" + book.getId() + "/" + imageId;
    }
}

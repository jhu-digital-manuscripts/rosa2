package rosa.iiif.presentation.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.util.Map;

/**
 * An implementation of ImageIdMapper that knows how to find an ID of
 * an image on the JHU FSI image server, given its location in the JHU
 * archive. An image id is FSI Share Name '/' Book Id '/' Image Id. 
 */
public class JhuFsiImageIdMapper implements ImageIdMapper {
    private final Map<String, String> fsi_share_map;

    /**
     * BookCollection names are mapped to fsi share names using the provided Map.
     * 
     * @param fsi_share_map map relating archive names to names on the image server
     */
    public JhuFsiImageIdMapper(Map<String, String> fsi_share_map) {
        this.fsi_share_map = fsi_share_map;
    }

    @Override
    public String mapId(BookCollection collection, Book book, String imageId) {
        String share = fsi_share_map.get(collection.getId());
        
        if (share == null) {
            share = collection.getId();
        }
        
        return  share + "/" + book.getId() + "/" + imageId;
    }
}

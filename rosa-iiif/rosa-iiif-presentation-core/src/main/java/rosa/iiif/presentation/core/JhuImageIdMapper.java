package rosa.iiif.presentation.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.util.Map;

/**
 * An implementation of ImageIdMapper that knows how to find an ID of
 * an image on the JHU image server, given its location in the JHU
 * archive. An image id is Collection Id '/' Book Id '/' Image Id (without extension).
 * Some images may also have a cropped version.
 */
public class JhuImageIdMapper implements ImageIdMapper {
    private final Map<String, String> fsi_share_map;

    /**
     * BookCollection names are mapped to fsi share names using the provided Map.
     * 
     * @param fsi_share_map map relating archive names to names on the image server
     */
    public JhuImageIdMapper(Map<String, String> fsi_share_map) {
        this.fsi_share_map = fsi_share_map;
    }

    @Override
    public String mapId(BookCollection collection, Book book, String imageId, boolean cropped) {
        String share = fsi_share_map.get(collection.getId());
        
        if (share == null) {
            share = collection.getId();
        }
        
        String image = imageId;
        int i = image.lastIndexOf('.');
        
        if (i > 0) {
           image = image.substring(0, i);
        }
        
        return  share + (book == null ? "" : "/" + book.getId()) + "/" + (cropped ? "cropped/" : "") + image;
    }
}

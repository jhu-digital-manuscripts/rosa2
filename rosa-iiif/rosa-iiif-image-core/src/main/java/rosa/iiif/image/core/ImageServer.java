package rosa.iiif.image.core;

import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;

public interface ImageServer {
    public String constructURL(ImageRequest req) throws IIIFException;

    public ImageInfo lookupImage(String image) throws IIIFException;

    int compliance();
}

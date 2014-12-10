package rosa.iiif.image.core;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.ImageServerProfile;

/**
 * Provides the ability to make IIIF requests to an image server that exposes a
 * HTTP API.
 * 
 * TODO Instead of constructing a URL, this interface should just perform the operation.
 */
public interface ImageServer {
    /**
     * Construct a URL that will invoke the requested image request on GET.
     * 
     * @param req
     * @return url
     * @throws IIIFException
     */
    public String constructURL(ImageRequest req) throws IIIFException;

    /**
     * Lookup information about an image.
     * 
     * @param image_id
     * @return ImageInfo or null
     * @throws IIIFException
     */
    public ImageInfo lookupImage(String image_id) throws IIIFException;

    /**
     * @return level of compliance with IIIF protocol
     */
    ComplianceLevel getCompliance();

    /**
     * @return supported capabilities not part of the compliance level
     */
    ImageServerProfile getProfile();
}

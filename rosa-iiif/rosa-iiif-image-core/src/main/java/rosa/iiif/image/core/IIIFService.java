package rosa.iiif.image.core;

import java.io.InputStream;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.ImageServerProfile;
import rosa.iiif.image.model.InfoRequest;

/**
 * Provides the ability to handle IIIF Image requests.
 */
public interface IIIFService {
    /**
     * Perform an image operation.
     * 
     * @param req
     * @return result
     * @throws IIIFException
     */
    public InputStream perform(ImageRequest req) throws IIIFException;

    /**
     * Perform an image info request. The resulting ImageInfo object will not
     * have the image uri set.
     * 
     * @param req
     * @return image info
     * @throws IIIFException
     */
    public ImageInfo perform(InfoRequest req) throws IIIFException;

    /**
     * @return whether or not the service can construct a URL that performs an
     *         image request.
     */
    public boolean supportsImageRequestURL();

    /**
     * The service may optionally be able to construct a URL such that a GET
     * request to it will perform the requested operation.
     * 
     * @param req
     * @return URL
     * @throws IIIFException
     */
    public String performURL(ImageRequest req) throws IIIFException;

    /**
     * @return level of compliance with IIIF protocol
     */
    ComplianceLevel getCompliance();

    /**
     * @return supported capabilities not part of the compliance level
     */
    ImageServerProfile getProfile();
}

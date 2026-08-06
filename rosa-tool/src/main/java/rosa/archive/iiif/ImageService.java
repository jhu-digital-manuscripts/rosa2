package rosa.archive.iiif;

/**
 * A IIIF Image API service descriptor referenced from Canvas painting annotations.
 *
 * <p>Describes how to access image tiles and derivatives for a Canvas image
 * through a IIIF Image API endpoint.
 *
 * @param id      the base URI of the image service
 * @param type    the service type (e.g. "ImageService2" or "ImageService3")
 * @param profile the compliance level profile URI of the image service
 */
public record ImageService(String id, String type, String profile) {}

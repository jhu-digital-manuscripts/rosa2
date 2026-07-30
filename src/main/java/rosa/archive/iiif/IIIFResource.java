package rosa.archive.iiif;

/**
 * A basic IIIF 3.0 resource with an identifier, type, and optional label.
 *
 * <p>This record represents the common properties shared by all IIIF resources
 * such as Manifests, Collections, Canvases, and Annotation Pages.
 *
 * @param id    the URI identifier of the resource
 * @param type  the IIIF resource type (e.g. "Manifest", "Collection", "Canvas")
 * @param label the human-readable label as a language map, or {@code null} if not applicable
 */
public record IIIFResource(String id, String type, LabelMap label) {}

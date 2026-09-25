package rosa.archive.iiif;

import java.util.List;
import java.util.Map;

/**
 * An immutable mapping of language codes to lists of label strings,
 * representing IIIF 3.0 language map objects (e.g. {@code {"en": ["Label text"]}}).
 *
 * @param labels a map from BCP 47 language tags (or {@code "none"}) to label values
 */
public record LabelMap(Map<String, List<String>> labels) {}

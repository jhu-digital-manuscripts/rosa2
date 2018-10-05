package rosa.iiif.presentation.core;

import java.util.HashSet;
import java.util.Set;

import rosa.archive.core.ArchiveNameParser;
import rosa.iiif.presentation.core.html.AdapterSet;
import rosa.iiif.presentation.core.html.AnnotationBaseHtmlAdapter;
import rosa.iiif.presentation.core.html.DrawingHtmlAdapter;
import rosa.iiif.presentation.core.html.GraphHtmlAdapter;
import rosa.iiif.presentation.core.html.MarginaliaHtmlAdapter;
import rosa.iiif.presentation.core.html.TableHtmlAdapter;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;

public class PresentationTestUtils {
    public static AnnotationTransformer annotationTransformer(PresentationUris presUris) {
        return new AnnotationTransformer(presUris, new ArchiveNameParser(), htmlAdapterSet(presUris));
    }

    public static AdapterSet htmlAdapterSet(PresentationUris presUris) {
        Set<AnnotationBaseHtmlAdapter<?>> adapters = new HashSet<>();

        adapters.add(new MarginaliaHtmlAdapter(presUris));
        adapters.add(new GraphHtmlAdapter(presUris));
        adapters.add(new TableHtmlAdapter(presUris));
        adapters.add(new DrawingHtmlAdapter(presUris));

        return new AdapterSet(adapters);
    }
}

package rosa.iiif.presentation.core;

import rosa.archive.core.ArchiveNameParser;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.core.transform.impl.AnnotationListTransformer;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.core.transform.impl.CanvasTransformer;
import rosa.iiif.presentation.core.transform.impl.LayerTransformer;
import rosa.iiif.presentation.core.transform.impl.ManifestTransformer;
import rosa.iiif.presentation.core.transform.impl.RangeTransformer;
import rosa.iiif.presentation.core.transform.impl.SequenceTransformer;
import rosa.iiif.presentation.core.transform.impl.TransformerSet;
import rosa.iiif.presentation.core.util.AdapterSet;
import rosa.iiif.presentation.core.util.AnnotationBaseHtmlAdapter;
import rosa.iiif.presentation.core.util.DrawingHtmlAdapter;
import rosa.iiif.presentation.core.util.GraphHtmlAdapter;
import rosa.iiif.presentation.core.util.MarginaliaHtmlAdapter;
import rosa.iiif.presentation.core.util.TableHtmlAdapter;

import java.util.HashSet;
import java.util.Set;

public class PresentationTestUtils {
    public static AnnotationTransformer annotationTransformer(IIIFPresentationRequestFormatter presReqFormatter) {
        return new AnnotationTransformer(presReqFormatter, new ArchiveNameParser(), htmlAdapterSet(presReqFormatter));
    }

    public static TransformerSet transformerSet(IIIFPresentationRequestFormatter presReqFormatter,
                                                IIIFRequestFormatter imageReqFormatter,
                                                ImageIdMapper idMapper) {
        Set<Transformer<?>> transformers = new HashSet<>();

        CanvasTransformer canvasTransformer = new CanvasTransformer(presReqFormatter, imageReqFormatter, idMapper);
        SequenceTransformer sequenceTransformer = new SequenceTransformer(presReqFormatter, canvasTransformer);
        AnnotationTransformer annotationTransformer = annotationTransformer(presReqFormatter);

        transformers.add(new AnnotationListTransformer(presReqFormatter, annotationTransformer));
        transformers.add(canvasTransformer);
        transformers.add(sequenceTransformer);
        transformers.add(new ManifestTransformer(presReqFormatter, sequenceTransformer, new RangeTransformer(presReqFormatter)));
        transformers.add(new RangeTransformer(presReqFormatter));
        transformers.add(new LayerTransformer(presReqFormatter));

        return new TransformerSet(transformers);
    }

    public static AdapterSet htmlAdapterSet(IIIFPresentationRequestFormatter formatter) {
        PresentationUris presUris = new PresentationUris(formatter);
        Set<AnnotationBaseHtmlAdapter<?>> adapters = new HashSet<>();

        adapters.add(new MarginaliaHtmlAdapter(presUris));
        adapters.add(new GraphHtmlAdapter(presUris));
        adapters.add(new TableHtmlAdapter(presUris));
        adapters.add(new DrawingHtmlAdapter(presUris));

        return new AdapterSet(adapters);
    }
}

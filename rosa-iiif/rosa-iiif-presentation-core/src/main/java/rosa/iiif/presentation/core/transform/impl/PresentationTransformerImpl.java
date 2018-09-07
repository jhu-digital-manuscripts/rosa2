package rosa.iiif.presentation.core.transform.impl;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.SimpleStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.ImageIdMapper;
import rosa.iiif.presentation.core.html.AdapterSet;
import rosa.iiif.presentation.core.html.AnnotationBaseHtmlAdapter;
import rosa.iiif.presentation.core.html.DrawingHtmlAdapter;
import rosa.iiif.presentation.core.html.GraphHtmlAdapter;
import rosa.iiif.presentation.core.html.MarginaliaHtmlAdapter;
import rosa.iiif.presentation.core.html.TableHtmlAdapter;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;


public class PresentationTransformerImpl extends BasePresentationTransformer implements PresentationTransformer {
    private CollectionTransformer col;
    private ManifestTransformer man;
    private SequenceTransformer seq;
    private CanvasTransformer canvas;
    private RangeTransformer range;
    private LayerTransformer lay;
    private AnnotationTransformer ann;
    private AnnotationListTransformer list;

    @Inject
    public PresentationTransformerImpl(@Named("formatter.presentation") IIIFPresentationRequestFormatter presFormatter,
            rosa.iiif.image.core.IIIFRequestFormatter imageFormatter, ImageIdMapper idMapper, SimpleStore store,
            ArchiveNameParser nameParser) {
        super(presFormatter);

        Set<AnnotationBaseHtmlAdapter<?>> html_adapters = new HashSet<>();
        
        html_adapters.add(new TableHtmlAdapter(pres_uris));
        html_adapters.add(new MarginaliaHtmlAdapter(pres_uris));
        html_adapters.add(new GraphHtmlAdapter(pres_uris));
        html_adapters.add(new DrawingHtmlAdapter(pres_uris));
        html_adapters.add(new TableHtmlAdapter(pres_uris));
        
        this.col = new CollectionTransformer(presFormatter, store, imageFormatter, idMapper);
        this.canvas = new CanvasTransformer(presFormatter, imageFormatter, idMapper);
        this.ann = new AnnotationTransformer(presFormatter, nameParser, new AdapterSet(html_adapters));
        this.range = new RangeTransformer(presFormatter);
        this.lay = new LayerTransformer(presFormatter);
        this.seq = new SequenceTransformer(presFormatter, canvas);
        this.list = new AnnotationListTransformer(presFormatter, ann);
        this.man = new ManifestTransformer(presFormatter, seq, range);
    }

    @Override
    public Manifest manifest(BookCollection collection, Book book) {
        return man.transform(collection, book, null);
    }

    @Override
    public Sequence sequence(BookCollection collection, Book book, String sequenceId) {
        return seq.transform(collection, book, sequenceId);
    }

    @Override
    public Canvas canvas(BookCollection collection, Book book, String page) {
        return canvas.transform(collection, book, page);
    }

    @Override
    public Range range(BookCollection collection, Book book, String name) {
        return range.transform(collection, book, name);
    }

    @Override
    public Layer layer(BookCollection collection, Book book, String name) {
        return lay.transform(collection, book, name);
    }

    @Override
    public AnnotationList annotationList(BookCollection collection, Book book, String name) {
        return list.transform(collection, book, name);
    }

    @Override
    public Collection collection(BookCollection collection) {
        return col.collection(collection);
    }
}

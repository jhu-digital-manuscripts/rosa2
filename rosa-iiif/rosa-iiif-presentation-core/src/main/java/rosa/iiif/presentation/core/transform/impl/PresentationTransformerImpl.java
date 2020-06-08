package rosa.iiif.presentation.core.transform.impl;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Inject;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFPresentationCache;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.html.AdapterSet;
import rosa.iiif.presentation.core.html.AnnotationBaseHtmlAdapter;
import rosa.iiif.presentation.core.html.CalculationHtmlAdapter;
import rosa.iiif.presentation.core.html.DrawingHtmlAdapter;
import rosa.iiif.presentation.core.html.GraphHtmlAdapter;
import rosa.iiif.presentation.core.html.MarginaliaHtmlAdapter;
import rosa.iiif.presentation.core.html.TableHtmlAdapter;
import rosa.iiif.presentation.core.transform.PresentationTransformer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;


public class PresentationTransformerImpl implements PresentationTransformer {
    private CollectionTransformer col;
    private ManifestTransformer man;
    private SequenceTransformer seq;
    private CanvasTransformer canvas;
    private RangeTransformer range;
    private AnnotationTransformer ann;
    private AnnotationListTransformer list;

    @Inject
    public PresentationTransformerImpl(IIIFPresentationCache cache, PresentationUris pres_uris, ArchiveNameParser nameParser) {
        Set<AnnotationBaseHtmlAdapter<?>> html_adapters = new HashSet<>();
        
        html_adapters.add(new TableHtmlAdapter(pres_uris));
        html_adapters.add(new MarginaliaHtmlAdapter(pres_uris));
        html_adapters.add(new GraphHtmlAdapter(pres_uris));
        html_adapters.add(new DrawingHtmlAdapter(pres_uris));
        html_adapters.add(new TableHtmlAdapter(pres_uris));
        html_adapters.add(new CalculationHtmlAdapter(pres_uris));
        
        this.col = new CollectionTransformer(cache, pres_uris);
        this.canvas = new CanvasTransformer(pres_uris);
        this.ann = new AnnotationTransformer(pres_uris, nameParser, new AdapterSet(html_adapters));
        this.range = new RangeTransformer(pres_uris);
        this.seq = new SequenceTransformer(pres_uris, canvas);
        this.list = new AnnotationListTransformer(pres_uris, ann);
        this.man = new ManifestTransformer(pres_uris, seq, range);
    }

    @Override
    public Manifest manifest(BookCollection collection, Book book) {
        return man.transform(collection, book);
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
    public AnnotationList annotationList(BookCollection collection, Book book, String name) {
        return list.transform(collection, book, name);
    }

    @Override
    public Collection collection(BookCollection collection) {
        return col.collection(collection);
    }
}

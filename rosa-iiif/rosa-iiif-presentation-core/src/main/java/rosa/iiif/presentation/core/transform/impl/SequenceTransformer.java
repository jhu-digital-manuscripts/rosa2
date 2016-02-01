package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.ViewingDirection;

import java.util.ArrayList;
import java.util.List;

public class SequenceTransformer extends BasePresentationTransformer implements Transformer<Sequence> {
    private CanvasTransformer canvasTransformer;

    @Inject
    public SequenceTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter, CanvasTransformer canvasTransformer) {
        super(presRequestFormatter);
        this.canvasTransformer = canvasTransformer;
    }

    @Override
    public Sequence transform(BookCollection collection, Book book, String name) {
        return buildSequence(collection, book, name, book.getImages());
    }

    @Override
    public Class<Sequence> getType() {
        return Sequence.class;
    }

    /**
     * Transform an archive image list into a IIIF sequence.
     *
     * @param imageList image list
     * @return sequence
     */
    private Sequence buildSequence(BookCollection collection, Book book, String label, ImageList imageList) {
        if (imageList == null) {
            return null;
        }

        Sequence sequence = new Sequence();
        sequence.setId(urlId(collection.getId(), book.getId(), label, PresentationRequestType.SEQUENCE));
        sequence.setType(IIIFNames.SC_SEQUENCE);
        sequence.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        sequence.setLabel(label, "en");

        List<Canvas> canvases = new ArrayList<>();
        int count = 0;
        boolean hasNotBeenSet = true;
        for (BookImage image : imageList) {
            String page = image.getName();
            canvases.add(canvasTransformer.transform(collection, book, image.getName()));

            // Set the starting point in the sequence to the first page of printed material
            if (hasNotBeenSet && page.matches(PAGE_REGEX)) {
                sequence.setStartCanvas(count);
                hasNotBeenSet = false;
            }

            count++;
        }
        sequence.setCanvases(canvases);

        // Set thumbnail for this sequence, set to the thumbnail for the start canvas
        if (sequence.getCanvases().size() > 0) {
            Canvas defaultCanvas = sequence.getCanvases().get(sequence.getStartCanvas());

            sequence.setThumbnailUrl(defaultCanvas.getThumbnailUrl());
            sequence.setThumbnailService(defaultCanvas.getThumbnailService());
        }

        return sequence;
    }
}

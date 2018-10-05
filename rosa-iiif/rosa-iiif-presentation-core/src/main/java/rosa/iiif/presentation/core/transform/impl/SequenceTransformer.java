package rosa.iiif.presentation.core.transform.impl;

import java.util.ArrayList;
import java.util.List;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.ImageList;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.ViewingDirection;

public class SequenceTransformer implements TransformerConstants {
    private CanvasTransformer canvasTransformer;
    private final PresentationUris pres_uris;
    
    public SequenceTransformer(PresentationUris pres_uris, CanvasTransformer canvasTransformer) {
        this.pres_uris = pres_uris;
        this.canvasTransformer = canvasTransformer;
    }

    public Sequence transform(BookCollection collection, Book book, String name) {
        // Default to cropped images
        ImageList images = book.getCroppedImages();
        boolean cropped = true;
        
        if (images == null) {
            images = book.getImages();
            cropped = false;
        }
        
        return buildSequence(collection, book, name, images, cropped);
    }


    /**
     * Transform an archive image list into a IIIF sequence.
     *
     * @param imageList image list
     * @param cropped 
     * @return sequence
     */
    private Sequence buildSequence(BookCollection collection, Book book, String label, ImageList imageList, boolean cropped) {
        if (imageList == null) {
            return null;
        }

        Sequence sequence = new Sequence();
        sequence.setId(pres_uris.getSequenceURI(collection.getId(), book.getId(), label));
        sequence.setType(IIIFNames.SC_SEQUENCE);
        sequence.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
        sequence.setLabel(label, "en");

        List<Canvas> canvases = new ArrayList<>();
        int count = 0;
        boolean hasNotBeenSet = true;
        
        for (BookImage image : imageList) {
            canvases.add(canvasTransformer.transform(collection, book, image, cropped));

            // Set the starting point in the sequence to the first page of printed material
            if (hasNotBeenSet && image.getLocation() != null &&
                    image.getLocation().equals(BookImageLocation.BODY_MATTER)) {
                sequence.setStartCanvas(count);
                hasNotBeenSet = false;
            }

            count++;
        }
        sequence.setCanvases(canvases);

        // Set thumbnail for this sequence, set to the thumbnail for the start canvas
        if (sequence.getCanvases().size() > 0 && sequence.getStartCanvas() >= 0) {
            Canvas defaultCanvas = sequence.getCanvases().get(sequence.getStartCanvas());

            if (defaultCanvas.getThumbnails().size() > 0) {
                sequence.addThumbnail(defaultCanvas.getThumbnails().get(0));
            }
        }

        return sequence;
    }
}

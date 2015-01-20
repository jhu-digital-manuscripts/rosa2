package rosa.iiif.presentation.model;

import java.util.ArrayList;
import java.util.List;

import rosa.iiif.presentation.model.annotation.Annotation;

/**
 * The canvas represents an individual page or view and acts as a central point for
 * laying out the different content resources that make up the display. Canvases must
 * be identified by a URI and it must be an HTTP(s) URI. If following the recommended
 * URI pattern, the {name} parameter must uniquely distinguish the canvas from all
 * other canvases in the object. As with sequences, the name should not begin with a
 * number. Suggested patterns are “f1r” or “p1”.
 *
 * Every canvas must have a label to display, and a height and a width as integers. A
 * canvas is a two-dimensional rectangular space with an aspect ratio that represents
 * a single logical view of some part of the object, and the aspect ratio is given with
 * the height and width properties. This allows resources to be associated with specific
 * parts of the canvas, rather than the entire space. Content must not be associated
 * with space outside of the canvas’s dimensions, such as at coordinates below 0,0 or
 * greater than the height or width.
 *
 * It is recommended that if there is (at the time of implementation) a single image that
 * depicts the page, then the dimensions of the image are used as the dimensions of the
 * canvas for simplicity. If there are multiple full images, then the dimensions of the
 * largest image should be used. If the largest image’s dimensions are less than 1200
 * pixels on either edge, then the canvas’s dimensions should be double that of the image.
 * Clients must be aware that this is not always the case, such as in the examples presented,
 * and instead must always scale images into the space represented by the canvas. The
 * dimensions of the canvas should be the same scale as the physical object, and thus images
 * should depict only the object. This can be accomplished by cropping the image, or
 * associating only a segment of the image with the canvas. The physical dimensions of the
 * object may be available via a service, either embedded within the description or requiring
 * an HTTP request to retrieve them.
 *
 * Image resources, and only image resources, are included in the images property of the canvas.
 * These are linked to the canvas via annotations. Other content, such as transcriptions, video,
 * audio or commentary, is provided via external annotation lists referenced in the otherContent
 * property. The value of both of these properties must be a list, even if there is only one
 * entry. Both are optional, as there may be no additional information associated with the canvas.
 * Note that the items in the otherContent list may be either objects with an @id property or
 * strings. In the case of a string, this is the URI of the annotation list and the type of
 * “sc:AnnotationList” can be inferred.
 *
 * In a sequence with the viewingHint value of “paged” and presented in a book viewing user
 * interface, the first canvas should be presented by itself – it is typically either the cover
 * or first recto page. Thereafter, the canvases represent the sides of the leaves, and hence
 * may be presented with two canvases displayed as an opening of the book. If there are canvases
 * which are in the sequence but would break this ordering, then they must have the viewingHint
 * property with a value of “non-paged”. Similarly if the first canvas is not a single up, it
 * must be marked as “non-paged” or an empty canvas added before it.
 *
 * Canvases may be dereferenced separately from the manifest via their URIs, and the following
 * representation information should be returned. This information should be embedded within the
 * sequence, as per previously.
 */
public class Canvas extends PresentationBase {
    private static final long serialVersionUID = 1L;

    private int height;
    private int width;

    private List<Annotation> images;
    private List<Reference> otherContent;

    public Canvas() {
        super();
        images = new ArrayList<>();
        otherContent = new ArrayList<>();

        setType(IIIFNames.SC_CANVAS);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public List<Annotation> getImages() {
        return images;
    }

    public void setImages(List<Annotation> images) {
        this.images = images;
    }

    public List<Reference> getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(List<Reference> otherContent) {
        this.otherContent = otherContent;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + height;
        result = prime * result + ((images == null) ? 0 : images.hashCode());
        result = prime * result + ((otherContent == null) ? 0 : otherContent.hashCode());
        result = prime * result + width;
        return result;
    }

    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Canvas;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Canvas))
            return false;
        Canvas other = (Canvas) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (height != other.height)
            return false;
        if (images == null) {
            if (other.images != null)
                return false;
        } else if (!images.equals(other.images))
            return false;
        if (otherContent == null) {
            if (other.otherContent != null)
                return false;
        } else if (!otherContent.equals(other.otherContent))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Canvas [height=" + height + ", width=" + width + ", images=" + images + ", otherContent="
                + otherContent + "]";
    }
}

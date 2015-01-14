package rosa.iiif.presentation.core.transform;

import org.json.JSONException;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.io.IOException;
import java.io.OutputStream;

public interface PresentationSerializer {

    /**
     * Write a IIIF presentation collection object to an output stream.
     *
     * @param collection IIIF presentation collection object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Collection collection, OutputStream out) throws IOException;

    /**
     * Write a IIIF presentation manifest to an output stream.
     *
     * @param manifest IIIF presentation manifest object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Manifest manifest, OutputStream out) throws IOException;

    /**
     * Write a IIIF presentation sequence to an output stream.
     *
     * @param sequence IIIF presentation sequence object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Sequence sequence, OutputStream out) throws IOException;

    /**
     * Write a IIIF presentation canvas to an output stream.
     *
     * @param canvas IIIF presentation canvas object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Canvas canvas, OutputStream out) throws IOException;

    /**
     * Write a IIIF presentation annotation to an output stream.
     *
     * @param annotation IIIF presentation annotation object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Annotation annotation, OutputStream out) throws IOException;

    /**
     * Write a IIIF presentation layer to an output stream.
     *
     * @param layer IIIF presentation layer object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Layer layer, OutputStream out)  throws IOException;

    /**
     * Write a IIIF presentation range to an output stream.
     *
     * @param range IIIF presentation range object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Range range, OutputStream out)  throws IOException;

    /**
     * Write a IIIF annotation list to an output stream.
     *
     * @param annotationList IIIF presentation annotation list object
     * @param os output stream to write to
     * @throws IOException
     */
    void write(AnnotationList annotationList, OutputStream os) throws IOException;

}

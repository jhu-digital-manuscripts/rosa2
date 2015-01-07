package rosa.iiif.presentation.core.transform;

import org.json.JSONException;
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
     *
     * @param collection IIIF presentation collection object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Collection collection, OutputStream out) throws JSONException, IOException;

    /**
     *
     * @param manifest IIIF presentation manifest object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Manifest manifest, OutputStream out) throws JSONException, IOException;

    /**
     *
     * @param sequence IIIF presentation sequence object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Sequence sequence, OutputStream out) throws JSONException, IOException;

    /**
     *
     * @param canvas IIIF presentation canvas object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Canvas canvas, OutputStream out) throws JSONException, IOException;

    /**
     *
     * @param annotation IIIF presentation annotation object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Annotation annotation, OutputStream out) throws JSONException, IOException;

    /**
     *
     * @param layer IIIF presentation layer object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Layer layer, OutputStream out)  throws JSONException, IOException;

    /**
     *
     * @param range IIIF presentation range object
     * @param out output stream to write to
     * @throws JSONException
     * @throws IOException
     */
    void write(Range range, OutputStream out)  throws JSONException, IOException;

}

package rosa.iiif.presentation.core.transform;

import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.io.OutputStream;

public interface Serializer {

    void write(Manifest manifest, OutputStream out);
    void write(Sequence sequence, OutputStream out);
    void write(Canvas canvas, OutputStream out);
    void write(Annotation annotation, OutputStream out);

}

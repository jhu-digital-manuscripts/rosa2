package rosa.iiif.presentation.core.transform;

import org.json.JSONException;
import org.json.JSONWriter;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.IIIFImageService;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationBase;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Sequence;
import rosa.iiif.presentation.model.Service;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;
import rosa.iiif.presentation.model.selector.Selector;
import rosa.iiif.presentation.model.selector.SvgSelector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

// TODO handle multiple languages?
public class JsonldSerializer implements PresentationSerializer {
    private static final String IIIF_PRESENTATION_CONTEXT = "http://iiif.io/api/presentation/2/context.json";

    public JsonldSerializer() {}

    @Override
    public void write(Collection collection, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(collection, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Manifest manifest, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(manifest, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Sequence sequence, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(sequence, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Canvas canvas, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(canvas, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Annotation annotation, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(annotation, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Range range, OutputStream os) throws JSONException, IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void write(Layer layer, OutputStream os) throws JSONException, IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /**
     * Add the IIIF presentation API context if it is needed.
     *
     * @param jWriter json-ld writer
     * @param included is this context included in the final document?
     * @throws JSONException
     */
    private void addIiifContext(JSONWriter jWriter, boolean included) throws JSONException {
        if (included) {
            jWriter.key("@context").value(IIIF_PRESENTATION_CONTEXT);
        }
    }

    private void writeJsonld(Collection collection, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(collection, jWriter);

        if (collection.getCollections().size() > 0) {
            jWriter.key("collections");
            jWriter.array();
            for (Collection c : collection.getCollections()) {
                writeJsonld(c, jWriter, false);
            }
            jWriter.endArray();
        }

        if (collection.getManifests().size() > 0) {
            jWriter.key("manifests");
            jWriter.array();
            for (Manifest man : collection.getManifests()) {
                jWriter.object();

                jWriter.key("@id").value(man.getId());
                jWriter.key("@type").value(man.getType());
                writeIfNotNull("label", man.getLabel("en"), jWriter);

                jWriter.endObject();
            }
            jWriter.endArray();
        }

        jWriter.endObject();
    }

    /**
     * Write out a IIIF Presentation Manifest as JSON-LD. All of the base data fields
     * that hold data are written. The JSON-LD representation of the Manifest object
     * embeds the default sequence (usually the first), and contains only a reference
     * to any other sequences.
     *
     * @param manifest IIIF Presentation manifest
     * @param jWriter JSON-LD writer
     */
    private void writeJsonld(Manifest manifest, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(manifest, jWriter);
        writeIfNotNull("viewingDirection", manifest.getViewingDirection(), jWriter);

        if (manifest.getSequences() != null) {
            jWriter.key("sequences");
            jWriter.array();
            writeJsonld(manifest.getSequences().get(manifest.getDefaultSequence()), jWriter, false);

            for (int i = 0; i < manifest.getSequences().size(); i++) {
                // Maybe the default sequence is not the first sequence in the list?
                if (i == manifest.getDefaultSequence()) {
                    continue;
                }

                Sequence seq = manifest.getSequences().get(i);
                jWriter.object();
                jWriter.key("@id").value(seq.getId());
                jWriter.key("@type").value(seq.getType());
                writeIfNotNull("label", seq.getLabel("en"), jWriter);
                jWriter.endObject();
            }
            jWriter.endArray();
        }
        jWriter.endObject();
    }

    /**
     * Write out a IIIF Presentation Sequence as JSON-LD. All base data fields that
     * contain data are written out. The JSON-LD representation embeds all canvases
     * contained within this sequence. The IIIF presentation api context is included
     * only if the initial request was for this sequence. Embedded sequences do not
     * need the context attached.
     *
     * @param sequence the sequence
     * @param jWriter JSON-LD writer
     */
    private void writeJsonld(Sequence sequence, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(sequence, jWriter);
        writeIfNotNull("viewingDirection", sequence.getViewingDirection(), jWriter);

        if (sequence.getStartCanvas() >= 0) {
            Canvas start = sequence.getCanvases().get(sequence.getStartCanvas());
            jWriter.key("startCanvas").value(start.getId());
        }

        jWriter.key("canvases");
        jWriter.array();
        for (Canvas canvas : sequence) {
            writeJsonld(canvas, jWriter, false);
        }
        jWriter.endArray();
        jWriter.endObject();
    }

    /**
     * Write out a IIIF Presentation Canvas as JSON-LD. All base data fields that
     * contain data are written. The JSON-LD representation embeds all image
     * annotations associated with this canvas. All other annotations are written
     * in an embedded annotation list. This annotation list holds references to
     * non-image annotations and can be referenced separately from the canvas.
     *
     * @param canvas the canvas
     * @param jWriter JSON-LD writer
     */
    private void writeJsonld(Canvas canvas, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(canvas, jWriter);
        writeIfNotNull("height", canvas.getHeight(), jWriter);
        writeIfNotNull("width", canvas.getWidth(), jWriter);

        if (canvas.getImages().size() > 0) {
            jWriter.key("images");
            jWriter.array();
            for (Annotation imageAnno : canvas.getImages()) {
                writeJsonld(imageAnno, jWriter, false);
            }
            jWriter.endArray();
        }

        if (canvas.getOtherContent().size() > 0) {
            String listId = canvas.getLabel("en"); // TODO tmp, need better way to get canvas label
            writeAnnotationList(canvas.getOtherContent(), listId, jWriter, false);
        }

        jWriter.endObject();
    }

    /**
     * Write an annotation as JSON-LD. All base data fields that contain data
     * are written. The JSON-LD representation has a type ('@type') defined by
     * its source. An annotation can potentially have multiple sources and
     * targets, each can be defined by a selector.
     *
     * @param annotation annotation
     * @param jWriter JSON-LD writer
     */
    private void writeJsonld(Annotation annotation, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(annotation, jWriter);
//        jWriter.key("@type").value(IIIFNames.OA_ANNOTATION);

        jWriter.key("resource");
        writeResource(annotation, jWriter);

        writeIfNotNull("motivation", annotation.getMotivation(), jWriter);

        // TODO write target with the possibility of it being a specific resource
        AnnotationTarget target = annotation.getDefaultTarget();
        jWriter.key("on").value(target.getUri());

        jWriter.endObject();
    }

    /**
     * Write an annotation list as JSON-LD. This object contains references to
     * annotations only.
     *
     * @param annoList list of annotations
     * @param id resolvable URL
     * @param jWriter JSON-LD writer
     */
    private void writeAnnotationList(List<Annotation> annoList, String id,
                                     JSONWriter jWriter, boolean isRequested) throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        jWriter.key("@id").value(id);
        jWriter.key("@type").value(IIIFNames.SC_ANNOTATION_LIST);

        jWriter.key("resources");
        jWriter.array();
        for (Annotation anno : annoList) {
            writeJsonld(anno, jWriter, false);
        }
        jWriter.endArray();
        jWriter.endObject();
    }

    /**
     * An annotation consists of a resource, or content source, and a target,
     * with which the source content is associated. In this resource, there can be
     * multiple content sources, in which case, the annotation has a choice of
     * which source to use.
     *
     * @param annotation a IIIF presentation annotation
     * @param jWriter JSON-LD writer
     * @throws JSONException
     */
    private void writeResource(Annotation annotation, JSONWriter jWriter) throws JSONException {
        jWriter.object();
        if (annotation.getSources().size() == 1) {
            writeSource(annotation.getDefaultSource(), annotation.getLabel("en"),
                    annotation.getWidth(), annotation.getHeight(), jWriter);
        } else {
            jWriter.key("@type").value(IIIFNames.OA_CHOICE);

            boolean isFirst = true;
            for (AnnotationSource source : annotation.getSources()) {
                if (isFirst) {
                    jWriter.key("default");
                    isFirst = false;
                } else {
                    jWriter.key("item");
                }

                jWriter.object();
                writeSource(source, annotation.getLabel("en"), annotation.getWidth(),
                        annotation.getHeight(), jWriter);
                jWriter.endObject();
            }
        }

        jWriter.endObject();
    }

    /**
     * Write an annotation source as JSON-LD.
     *
     * @param source content source for an annotation
     * @param label label to give the source
     * @param width width, if source is an image
     * @param height height, if source is an image
     * @param jWriter JSON-LD writer
     * @throws JSONException
     */
    private void writeSource(AnnotationSource source, String label, int width,
                             int height, JSONWriter jWriter) throws JSONException {
        writeIfNotNull("label", label, jWriter);
        if (source.isEmbeddedText()) {
            jWriter.key("@type").value(IIIFNames.CNT_CONTENT_AS_TEXT);
            jWriter.key("chars").value(source.getEmbeddedText());
        } else if (source.isImage()) {
            jWriter.key("@type").value(IIIFNames.DC_IMAGE);
            writeIfNotNull("format", source.getFormat(), jWriter);
            writeIfNotNull("width", width, jWriter);
            writeIfNotNull("height", height, jWriter);
            writeService(source.getService(), jWriter);
        }

        if (source.isSpecificResource()) {
            writeSelector(source.getSelector(), jWriter);
        }
    }

    private void writeSelector(Selector selector, JSONWriter jWriter) throws JSONException {
        jWriter.key("selector");
        jWriter.object();
        writeIfNotNull("@context", selector.context(), jWriter);
        // TODO not very flexible for new selectors...
        if (selector instanceof SvgSelector) {
            jWriter.key("@type");

            jWriter.array();
            jWriter.value(selector.type());
            jWriter.value(IIIFNames.CNT_CONTENT_AS_TEXT);
            jWriter.endArray();

            jWriter.key("chars").value(selector.content());
        } else if (selector instanceof FragmentSelector) {
            jWriter.key("@type").value(selector.type());
            jWriter.key("region").value(selector.content());
        }

        jWriter.endObject();
    }

    /**
     * Write out the set of data shared by IIIF Presentation objects.
     *
     * @param obj IIIF Presentation model object
     * @param jWriter JSON-LD writer
     * @param <T> type
     * @throws JSONException
     */
    private <T extends PresentationBase> void writeBaseData(T obj, JSONWriter jWriter)
            throws JSONException {
        jWriter.key("@id").value(obj.getId());
        jWriter.key("@type").value(obj.getType());

        writeIfNotNull("label", obj.getLabel("en"), jWriter);
        writeIfNotNull("description", obj.getDescription("en"), jWriter);
        writeIfNotNull("viewingHint", obj.getViewingHint(), jWriter);

        if (obj.getMetadata() != null && obj.getMetadata().size() > 0) {
            jWriter.key("metadata");
            jWriter.array();

            for (String mKey : obj.getMetadata().keySet()) {
                jWriter.object();
                jWriter.key("label").value(mKey);
                jWriter.key("value").value(obj.getMetadata().get(mKey).asString());
                jWriter.endObject();
            }

            jWriter.endArray();
        }

        if (obj.getThumbnailUrl() != null) {
            jWriter.key("thumbnail");
            jWriter.object();

            jWriter.key("@id").value(obj.getThumbnailUrl());
            writeService(obj.getThumbnailService(), jWriter);

            jWriter.endObject();
        }

        // Rights info
        writeIfNotNull("license", obj.getLicense(), jWriter);
        writeIfNotNull("attribution", obj.getAttribution("en"), jWriter);
        writeIfNotNull("logo", obj.getLogo(), jWriter);

        // Links
        if (obj.getRelatedUri() != null) {
            jWriter.key("related");
            jWriter.object();

            jWriter.key("@id").value(obj.getRelatedUri());
            writeIfNotNull("format", obj.getRelatedFormat(), jWriter);

            jWriter.endObject();
        }
        writeService(obj.getService(), jWriter);
        writeIfNotNull("seeAlso", obj.getSeeAlso(), jWriter);
        writeIfNotNull("within", obj.getWithin(), jWriter);
    }

    private void writeService(Service service, JSONWriter jWriter) throws JSONException {
        if (service == null) {
            return;
        }

        jWriter.key("service");
        jWriter.object();

        writeIfNotNull("@context", service.getContext(), jWriter);
        writeIfNotNull("@id", service.getId(), jWriter);
        writeIfNotNull("profile", service.getProfile(), jWriter);

        if (service instanceof IIIFImageService) {
            IIIFImageService imageService = (IIIFImageService) service;

            writeIfNotNull("height", imageService.getHeight(), jWriter);
            writeIfNotNull("width", imageService.getWidth(), jWriter);

            if (imageService.getTileHeight() != -1 || imageService.getTileWidth() != -1
                    || imageService.getScaleFactors() != null) {
                jWriter.key("tiles");
                jWriter.array();
                jWriter.object();

                writeIfNotNull("width", imageService.getTileWidth(), jWriter);
                writeIfNotNull("height", imageService.getTileHeight(), jWriter);
                if (imageService.getScaleFactors() != null) {
                    jWriter.key("scaleFactors");
                    jWriter.array();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < imageService.getScaleFactors().length; i++) {
                        if (i != 0) {
                            sb.append(',');
                        }
                        sb.append(imageService.getScaleFactors()[i]);
                    }
                    jWriter.endArray();
                }
                jWriter.endObject();
                jWriter.endArray(); // TODO the hell is with this structure (its from the IIIF spec)??
            }
        }
        jWriter.endObject();
    }

    private void writeIfNotNull(String key, Object value, JSONWriter jWriter)
            throws JSONException {
        if (value != null && !value.toString().equals("")) {
            jWriter.key(key).value(value.toString());
        }
    }

    private void writeIfNotNull(String key, int value, JSONWriter jWriter) throws JSONException {
        if (value != -1) {
            jWriter.key(key).value(value);
        }
    }

}

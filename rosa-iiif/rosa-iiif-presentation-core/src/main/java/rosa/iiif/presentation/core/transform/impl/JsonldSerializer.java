package rosa.iiif.presentation.core.transform.impl;

import org.json.JSONException;
import org.json.JSONWriter;

import rosa.iiif.presentation.core.transform.PresentationSerializer;
import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.Canvas;
import rosa.iiif.presentation.model.Collection;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Layer;
import rosa.iiif.presentation.model.Manifest;
import rosa.iiif.presentation.model.PresentationBase;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.Reference;
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

// TODO handle multiple languages?
public class JsonldSerializer implements PresentationSerializer, IIIFNames {
    private static final String IIIF_PRESENTATION_CONTEXT = "http://iiif.io/api/presentation/2/context.json";

    /**
     * Create a JsonldSerializer
     */
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
    public void write(AnnotationList annotationList, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(annotationList, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Range range, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(range, jWriter, true);
        writer.flush();
    }

    @Override
    public void write(Layer layer, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(layer, jWriter, true);
        writer.flush();
    }

    /**
     * Add the IIIF presentation API context if it is needed.
     *
     * @param jWriter json-ld writer
     * @param included is this context included in the final document?
     * @throws JSONException
     */
    protected void addIiifContext(JSONWriter jWriter, boolean included) throws JSONException {
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
            for (Reference ref : collection.getCollections()) {
                writeJsonld(ref, jWriter);
            }
            jWriter.endArray();
        }

        if (collection.getManifests().size() > 0) {
            jWriter.key("manifests");
            jWriter.array();
            for (Reference ref : collection.getManifests()) {
                writeJsonld(ref, jWriter);
            }
            jWriter.endArray();
        }

        jWriter.endObject();
    }
    
    private void writeJsonld(Range range, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(range, jWriter);

        if (!range.getCanvases().isEmpty()) {
            jWriter.key("canvases");
            jWriter.array();
            for (String s : range.getCanvases()) {
                jWriter.value(s);
            }
            jWriter.endArray();
        }
        
        if (!range.getRanges().isEmpty()) {
            jWriter.key("ranges");
            jWriter.array();
            for (String s : range.getRanges()) {
                jWriter.value(s);
            }
            jWriter.endArray();
        }

        jWriter.endObject();
    }

    private void writeJsonld(Reference ref, JSONWriter jWriter) {
        jWriter.object();

        jWriter.key("@id").value(ref.getReference());
        jWriter.key("@type").value(ref.getType());
        jWriter.key("label").value(ref.getLabel().getValue());

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
     * @param isRequested was this object requested directly?
     */
    private void writeJsonld(Manifest manifest, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(manifest, jWriter);
        writeIfNotNull("viewingDirection",
                manifest.getViewingDirection() != null ? manifest.getViewingDirection().getKeyword() : null, jWriter);

        if (manifest.getDefaultSequence() == null && (manifest.getOtherSequences() == null
                || manifest.getOtherSequences().isEmpty())) {

        } else {
            jWriter.key("sequences").array();

            if (manifest.getDefaultSequence() != null) {
                writeJsonld(manifest.getDefaultSequence(), jWriter, false);
            }
            for (Reference ref : manifest.getOtherSequences()) {
                writeJsonld(ref, jWriter);
            }

            jWriter.endArray();
        }
        
        if (!manifest.getRanges().isEmpty()) {
            jWriter.key("structures").array();

            for (Range range: manifest.getRanges()) {
                if (range != null) { // TODO find out what is generating NULL ranges...
                    writeJsonld(range, jWriter, false);
                }
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
     * @param isRequested was this object requested directly?
     */
    private void writeJsonld(Sequence sequence, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(sequence, jWriter);
        writeIfNotNull("viewingDirection",
                sequence.getViewingDirection() != null ? sequence.getViewingDirection().getKeyword() : null, jWriter);

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
     * @param isRequested was this object requested directly?
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

        if (canvas.getOtherContent() != null && canvas.getOtherContent().size() > 0) {
            jWriter.key("otherContent").array();
            for (Reference ref : canvas.getOtherContent()) {
                writeJsonld(ref, jWriter);
            }
            jWriter.endArray();
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
     * @param isRequested was this object requested directly?
     */
    protected void writeJsonld(Annotation annotation, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(annotation, jWriter);

        jWriter.key("resource");
        writeResource(annotation, jWriter);

        writeIfNotNull("motivation", annotation.getMotivation(), jWriter);

        // TODO write target with the possibility of it being a specific resource
        writeTarget(annotation, jWriter);
//        AnnotationTarget target = annotation.getDefaultTarget();
//        jWriter.key("on").value(target.getUri());

        jWriter.endObject();
    }

    /**
     * Write an annotation list as JSON-LD. This object contains references to
     * annotations only. Annotation lists are not embedded in a manifest, so the
     * annotations contained within this annotation list is only included if
     * the annotation list is requested separately.
     *
     * @param annoList annotation list object
     * @param jWriter JSON-LD writer
     * @param isRequested was this object requested directly?
     */
    private void writeJsonld(AnnotationList annoList, JSONWriter jWriter, boolean isRequested)
            throws JSONException {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        jWriter.key("@id").value(annoList.getId());
        jWriter.key("@type").value(IIIFNames.SC_ANNOTATION_LIST);

        if (isRequested) {
            jWriter.key("resources").array();
            for (Annotation anno : annoList) {
                writeJsonld(anno, jWriter, false);
            }
            jWriter.endArray();
        }
        jWriter.endObject();
    }

    /**
     * Write a layer as JSON-LD. This object holds a list of URIs referencing
     * annotation lists.
     *
     * @param layer a IIIF presentation layer
     * @param jWriter JSON-LD writer
     * @param isRequested was this object requested directly?
     */
    private void writeJsonld(Layer layer, JSONWriter jWriter, boolean isRequested) {
        jWriter.object();

        addIiifContext(jWriter, isRequested);
        writeBaseData(layer, jWriter);

        jWriter.key("otherContent").array();
        for (String uri : layer.getOtherContent()) {
            jWriter.value(uri);
        }
        jWriter.endArray().endObject();
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
        jWriter.key("@id").value(source.getUri());
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
        writeIfNotNull("label", label, jWriter);

        if (source.isSpecificResource()) {
            writeSelector(source.getSelector(), jWriter);
        }
    }

    protected void writeTarget(Annotation annotation, JSONWriter jWriter) throws JSONException {
        AnnotationTarget target = annotation.getDefaultTarget();

        if (target.isSpecificResource()) {
            Selector selector = target.getSelector();
            if (selector instanceof FragmentSelector) {
                jWriter.key("on").value(target.getUri() + "#xywh=" + selector.content());
            } else if (selector instanceof SvgSelector) {
                writeSelector(target.getSelector(), jWriter);
            }
        } else {
            jWriter.key("on").value(target.getUri());
        }

    }

    protected void writeSelector(Selector selector, JSONWriter jWriter) throws JSONException {
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
    protected  <T extends PresentationBase> void writeBaseData(T obj, JSONWriter jWriter)
            throws JSONException {
        jWriter.key("@id").value(obj.getId());
        jWriter.key("@type").value(obj.getType());

        writeIfNotNull("label", obj.getLabel("en"), jWriter);
        writeIfNotNull("description", obj.getDescription("en"), jWriter);
        writeIfNotNull("viewingHint", obj.getViewingHint() != null ? obj.getViewingHint().getKeyword() : null, jWriter);

        if (obj.getMetadata() != null && obj.getMetadata().size() > 0) {
            jWriter.key("metadata");
            jWriter.array();

            for (String mKey : obj.getMetadata().keySet()) {
                jWriter.object();
                jWriter.key("label").value(mKey);
                jWriter.key("value").value(obj.getMetadata().get(mKey).getValue());
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

//        if (service instanceof IIIFImageService) {
//            IIIFImageService imageService = (IIIFImageService) service;
//
//            writeIfNotNull("height", imageService.getHeight(), jWriter);
//            writeIfNotNull("width", imageService.getWidth(), jWriter);
//
//            if (imageService.getTileHeight() != -1 || imageService.getTileWidth() != -1
//                    || imageService.getScaleFactors() != null) {
//                jWriter.key("tiles");
//                jWriter.array();
//                jWriter.object();
//
//                writeIfNotNull("width", imageService.getTileWidth(), jWriter);
//                writeIfNotNull("height", imageService.getTileHeight(), jWriter);
//                if (imageService.getScaleFactors() != null) {
//                    jWriter.key("scaleFactors");
//                    jWriter.array();
//                    StringBuilder sb = new StringBuilder();
//                    for (int i = 0; i < imageService.getScaleFactors().length; i++) {
//                        if (i != 0) {
//                            sb.append(',');
//                        }
//                        sb.append(imageService.getScaleFactors()[i]);
//                    }
//                    jWriter.endArray();
//                }
//                jWriter.endObject();
//                jWriter.endArray(); // TODO the hell is with this structure (its from the IIIF spec)??
//            }
//        }
        jWriter.endObject();
    }

    protected void writeIfNotNull(String key, Object value, JSONWriter jWriter)
            throws JSONException {
        if (value != null && !value.toString().equals("")) {
            jWriter.key(key).value(value.toString());
        }
    }

    protected void writeIfNotNull(String key, int value, JSONWriter jWriter) throws JSONException {
        if (value != -1) {
            jWriter.key(key).value(value);
        }
    }

}

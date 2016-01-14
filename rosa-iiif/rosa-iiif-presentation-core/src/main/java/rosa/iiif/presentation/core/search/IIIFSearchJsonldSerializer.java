package rosa.iiif.presentation.core.search;

import org.json.JSONException;
import org.json.JSONWriter;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchNames;
import rosa.iiif.presentation.model.search.IIIFSearchResult;
import rosa.iiif.presentation.model.selector.FragmentSelector;
import rosa.iiif.presentation.model.selector.Selector;
import rosa.iiif.presentation.model.selector.SvgSelector;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class IIIFSearchJsonldSerializer extends JsonldSerializer implements IIIFNames, IIIFSearchNames {

    public IIIFSearchJsonldSerializer() {}

    public void write(IIIFSearchResult object, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);

        writeJsonld(object, jWriter, true);
        writer.flush();
    }

    @Override
    protected void addIiifContext(JSONWriter writer, boolean isRequested) {
        if (isRequested) {
            writer.key("@context").array();
            writer.value(IIIFNames.IIIF_PRESENTATION_CONTEXT);
            writer.value(IIIFSearchNames.SEARCH_CONTEXT);
            writer.endArray();
        }
    }

    private void writeJsonld(IIIFSearchResult result, JSONWriter writer, boolean isRequested) {
        writer.object();

        addIiifContext(writer, isRequested);
        writeBaseData(result, writer);

        writer.key("within").object();
        writer.key("type").value(SC_LAYER);
        writer.key("total").value(result.getTotal());

        // TODO make IGNORED param configurable
        writer.key("ignored").array();
        for (String ig : result.getIgnored()) {
            writer.value(ig);
        }
        writer.endArray();

        // TODO must generate/retrieve First/Last URIs
        writeIfNotNull("first", result.getFirst(), writer);
        writeIfNotNull("last", result.getLast(), writer);

        writer.endObject();      // end 'WITHIN'

        writeIfNotNull("next", result.getNext(), writer);
        writeIfNotNull("prev", result.getPrev(), writer);
        writeIfNotNull("startIndex", result.getStartIndex(), writer);

        if (isRequested) {
            writer.key("resources").array();
            for (Annotation anno : result) {
                writeJsonld(anno, writer, false);
            }
            writer.endArray();
        }

        // Create "hits" obj
        if (result.getHits() != null) {
            writer.key("hits").array();
            for (IIIFSearchHit hit : result.getHits()) {
                writeJsonld(hit, writer);
            }
            writer.endArray();
        }

        writer.endObject();
    }

    private void writeJsonld(IIIFSearchHit hit, JSONWriter writer) {
        if (hit == null) {
            return;
        }
        writer.object();

        writer.key("type").value(IIIFSearchNames.SEARCH_HIT);
        writer.key("annotations").array();
        for (String anno : hit.annotations) {
            writer.value(anno);     // TODO ensure these are URIs
        }
        writer.endArray();

        writeIfNotNull("match", hit.matching, writer);
        writeIfNotNull("before", hit.before, writer);
        writeIfNotNull("after", hit.after, writer);

        writer.endObject();
    }

    /**
     * Modify existing behavior when writing out AoR annotations to JSON by
     * including a modified "on" parameter that includes a "within" property
     * to specify the parent objects.
     *
     * @param annotation annotation
     * @param writer JSON writer
     * @throws JSONException
     */
    @Override
    protected void writeTarget(Annotation annotation, JSONWriter writer) throws JSONException {
        AnnotationTarget target = annotation.getDefaultTarget();

        if (target.isSpecificResource()) {
            Selector selector = target.getSelector();
            if (selector instanceof FragmentSelector) {
                writer.key("on").object().key("@id").value(target.getUri() + "#xywh=" + selector.content());
                writeWithin(target, writer);
                writer.endObject();
            } else if (selector instanceof SvgSelector) {
                writeSelector(target.getSelector(), writer);
            }
        } else {
            writer.key("on").object().key("@id").value(target.getUri());
            writeWithin(target, writer);
            writer.endObject();
        }
    }

    private void writeWithin(AnnotationTarget target, JSONWriter writer) {
        Reference ref = target.getParentRef();
        if (ref == null) {
            return;
        }

        writer.key("within").object();

        writer.key("@id").value(ref.getReference());
        writer.key("type").value(ref.getType());
        writer.key("label").value(ref.getLabel());

        writer.endObject();
    }
}

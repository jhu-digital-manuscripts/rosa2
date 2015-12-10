package rosa.iiif.search.core;

import org.json.JSONException;
import org.json.JSONWriter;
import rosa.iiif.presentation.core.transform.impl.JsonldSerializer;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.search.model.IIIFSearchNames;
import rosa.iiif.search.model.IIIFSearchResult;

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
        writer.key("@id").value(result.getId());
        writer.key("@type").value(IIIFNames.SC_ANNOTATION_LIST);

        writer.key("within").object();
        writer.key("type").value(SC_LAYER);
        writer.key("total").value(result.getTotal());

        writer.key("ignored").array()
                .value("date").value("user").value("box")
                .endArray();

        if (isRequested) {
            writer.key("resources").array();
            for (Annotation anno : result) {
                writeJsonld(anno, writer, false);
            }
            writer.endArray();
        }
        writer.endObject();
    }
}

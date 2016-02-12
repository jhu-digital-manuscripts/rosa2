package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;

import rosa.iiif.presentation.model.IIIFNames;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchResult;

public class JHSearchSerializer implements IIIFNames {

    public JHSearchSerializer() {}

    public void write(String request_url, String query, SearchResult result, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter jWriter = new JSONWriter(writer);
        writeJsonld(request_url, query, result, jWriter);
        writer.flush();
    }

    private void writeJsonld(String request_url, String query, SearchResult result, JSONWriter writer) {
        writer.object();
        writer.key("@context").value(JHSearchService.CONTEXT_URI);
        
        writer.key("@id").value(request_url);
        writer.key("@type").value("jhiff:SearchResult");
        
        writer.key("query").value(query);
        writer.key("debug").value(result.getDebugMessage());
        
        if (result.getResumeToken() != null) {
            writer.key("resume").value(result.getResumeToken());
        }

        writer.key("offset").value(result.getOffset());
        writer.key("total").value(result.getTotal());

        writer.key("matches").array();
        for (SearchMatch match: result.getMatches()) {
            writeJsonld(match, writer);
        }
        writer.endArray();

        writer.endObject();
    }

    // Callback to get html?
    
    private void writeJsonld(SearchMatch match, JSONWriter writer) {
        writer.object();

        StringBuilder context_html = new StringBuilder();        
        List<String> context = match.getContext();
        
        for (int i = 0; i < context.size(); ) {
            String field = context.get(i++);
            String html = context.get(i++);
            
            context_html.append("<p><b>" + field + ":</b>" + html + "</p>");
        }

        List<String> values = match.getValues();
        
        String object_type = null;
        String object_label = null;
        String manifest_id = null;
        String manifest_label = null;
        
        for (int i = 0; i < values.size(); ) {
            String field = values.get(i++);
            String value = values.get(i++);
            
            if (field.equals(JHSearchFields.OBJECT_TYPE.getFieldName())) {
                object_type = value;
            } else if (field.equals(JHSearchFields.OBJECT_LABEL.getFieldName())) {
                object_label = value;
            } else if (field.equals(JHSearchFields.MANIFEST_ID.getFieldName())) {
                manifest_id = value;
            } else if (field.equals(JHSearchFields.MANIFEST_LABEL.getFieldName())) {
                manifest_label = value;
            }
        }
        
        writer.key("context").value(context_html.toString());
        
        writer.key("object").object();
        writer.key("@id").value(match.getId());
        
        if (object_type != null) {
            writer.key("@type").value(object_type);
        }
        
        if (object_label != null) {
            writer.key("label").value(object_label);
        }
        
        writer.endObject();
        
        if (manifest_id != null) {
            writer.key("manifest").object();
            writer.key("@id").value(manifest_id);
            writer.key("@type").value(IIIFNames.SC_MANIFEST);
            
            if (manifest_label != null) {
                writer.key("label").value(manifest_label);
            }
            writer.endObject();
        }
        
        writer.endObject();
    }
}

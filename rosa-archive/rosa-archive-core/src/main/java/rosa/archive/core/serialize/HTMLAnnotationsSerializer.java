package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import rosa.archive.core.ArchiveConstants;
import rosa.archive.model.HTMLAnnotations;

/**
 * Use Web Annotation JSON-LD as the serialization format.
 * Assume it is JSON array of Web Annotation objects which look like:
 * <pre>
 * {
 * "@context": "http://www.w3.org/ns/anno.jsonld",
 * "id": "http://example.org/anno5",
 * "type": "Annotation",
 * "via" : "http://example.com/blah.html",
 * "body": {
 *   "type" : "TextualBody",
 *   "value" : "<p>Moo!</p>",
 *   "format" : "text/html",
 * },
 * "target": "rosa:ARCHIVE_ID"
 * }
 * </pre>
 * 
 * The target uri must use the rosa schema and have a path which is the archive id.
 * The via statement can be used to indicate where this annotation was obtained.
 */
public class HTMLAnnotationsSerializer implements Serializer<HTMLAnnotations>, ArchiveConstants {
	private static final String ARCHIVE_URI_SCHEME = "rosa";
	
    @Override
    public HTMLAnnotations read(InputStream is, List<String> errors) throws IOException {
       HTMLAnnotations result = new HTMLAnnotations();
       
       JSONArray array = new JSONArray(new JSONTokener(is));

       for (int i = 0; i < array.length(); i++) {
    	   JSONObject obj = array.getJSONObject(i);
    	   
    	   String context = obj.getString("@context");
    	   String type = obj.getString("type");
    	   String id = obj.getString("id");
    	   String target = obj.getString("target");
    	   
    	   JSONObject body_obj = obj.getJSONObject("body");
    	   
    	   if (context == null || !context.equals("http://www.w3.org/ns/anno.jsonld")) {
    		   throw new IOException("Web Annotation has invalid context: " + obj);
    	   }
    	   
    	   if (type == null || !type.equals("Annotation")) {
    		   throw new IOException("Web Annotation has invalid type: " + obj);
    	   }
    	   
    	   String body_type = body_obj.getString("type");
    	   String body_value = body_obj.getString("value");
    	   String body_format = body_obj.getString("format");

    	   if (body_type == null || !body_type.equals("TextualBody")) {
    		   throw new IOException("Web Annotation body has unsupported type: " + obj);
    	   }
    	   
    	   if (body_format == null || !body_format.equals("text/html")) {
    		   throw new IOException("Web Annotation body has unsupported format: " + obj);
    	   }
    	   
    	   if (body_value== null) {
    		   throw new IOException("Web Annotation body missing value: " + obj);
    	   }
    	   
    	   if (!target.startsWith(ARCHIVE_URI_SCHEME)) {
    		   throw new IOException("Unsupported Web Annotation target: " + obj);
    	   }
    	   
    	   String archive_id = target.substring(ARCHIVE_URI_SCHEME.length() + 1);

    	   result.setAnnotation(archive_id, body_value);
       }
       
       return result;
    }

    @Override
    public void write(HTMLAnnotations map, OutputStream out) throws IOException {
    	throw new UnsupportedOperationException();
    }

    @Override
    public Class<HTMLAnnotations> getObjectType() {
        return HTMLAnnotations.class;
    }
}

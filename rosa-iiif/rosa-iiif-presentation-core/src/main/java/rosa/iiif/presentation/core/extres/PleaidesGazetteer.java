package rosa.iiif.presentation.core.extres;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class PleaidesGazetteer extends SimpleExternalResourceDb {
    private static String pleiades_res_path = "/pleiades-places-latest.json.gz";
    private static Logger logger = Logger.getLogger(PleaidesGazetteer.class.getName());

    public PleaidesGazetteer(InputStream is) throws IOException {
        JsonFactory factory = new MappingJsonFactory();

        JsonParser parser = factory.createParser(new GZIPInputStream(is));

        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected object start");
        }

        // Process each node in graph.
        // Load single node at a time.

        findKey(parser, "@graph");

        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected @graph array");
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            process(parser.readValueAsTree());
        }
    }

    public PleaidesGazetteer() throws IOException {
        this(PleaidesGazetteer.class.getResourceAsStream(pleiades_res_path));
    }

    private static void findKey(JsonParser parser, String key) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.getCurrentName().equals(key)) {
                return;
            }
            
            parser.skipChildren();
        }

        throw new IOException("Could not find key: " + key);
    }

    private String get(JsonNode node, String key) {
        return node.has(key) ? node.get(key).asText() : null;
    }

    // Get all terms from a string, splits on /
    private String[] get_terms(String s) {
        if (s == null) {
            return new String[] {};
        }

        return s.split("/");
    }

    private String[] get_terms(JsonNode node, String key) {
        return get_terms(get(node, key));
    }

    // Remove punctuation and trailing whitespace
    public String normalize(String s) {
        return s.replaceAll("\\p{Punct}+", "").toLowerCase().trim();
    }


    // Grab title and uri from the object, then also any names.
    private void process(JsonNode node) {
        URI uri = null;

        try {
            String s = get(node, "uri");

            if (s == null) {
                return;
            }

            uri = new URI(s);
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Invalid URI", e);
            return;
        }

        for (String term : get_terms(node, "title")) {
            add(term, uri);
        }

        String uri_str = get(node, "uri");

        if (uri_str == null) {
            return;
        }

        if (node.has("names")) {
            JsonNode names = node.get("names");
            
            if (names.isArray()) {
                for (JsonNode name: names) {
                    for (String term : get_terms(name, "romanized")) {
                        add(term, uri);
                    }    
                }
            }
        }
    }
}

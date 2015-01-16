package rosa.archive.core.util;

import org.apache.commons.io.IOUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;

public class CachingUrlEntityResolver implements EntityResolver {
    private static final String ENCODING = "UTF-8";
    private static final int CACHE_MAX_SIZE = 1000;

    private ConcurrentHashMap<String, String> entityCache;

    public CachingUrlEntityResolver() {
        this.entityCache = new ConcurrentHashMap<>();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource is = new InputSource(systemId);

        if (!entityCache.containsKey(systemId)) {
            URL url = new URL(systemId);

            String i = getLocalCopy(url);
            if (i == null) {
                // Make network call only if local copy does not exist
                i = IOUtils.toString(url, ENCODING);
            }

            if (entityCache.size() > CACHE_MAX_SIZE) {
                entityCache.clear();
            }

            entityCache.putIfAbsent(systemId, i);
            is.setByteStream(IOUtils.toInputStream(i, ENCODING));
        }

        is.setByteStream(IOUtils.toInputStream(entityCache.get(systemId), ENCODING));
        return is;
    }

    private String getLocalCopy(URL systemId) throws IOException {
        String[] parts = URLDecoder.decode(systemId.getPath(), ENCODING).split("/");

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(parts[parts.length - 1])) {
            if (in != null) {
                return IOUtils.toString(in, ENCODING);
            }
        }

        return null;
    }
}
